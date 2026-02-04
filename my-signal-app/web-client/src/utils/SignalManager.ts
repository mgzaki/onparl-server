import { SignalProtocolAddress, SessionBuilder, SessionCipher, KeyHelper } from '@privacyresearch/libsignal-protocol-typescript';
import { InMemorySignalProtocolStore } from './InMemorySignalProtocolStore';
import axios from 'axios';

export const SERVER_URL = 'https://d30popws4xs094.cloudfront.net';

export class SignalManager {
    private store: InMemorySignalProtocolStore;
    private userId: string;

    constructor(userId: string) {
        this.userId = userId;
        this.store = new InMemorySignalProtocolStore(userId);
    }

    async initialize(): Promise<void> {
        console.log('[SignalManager] Initializing for user:', this.userId);

        try {
            // Check if keys already exist (restored from storage)
            const existingId = await this.store.getIdentityKeyPair();
            const existingRegId = await this.store.getLocalRegistrationId();

            if (existingId && existingRegId) {
                console.log('[SignalManager] Keys found in storage. Skipping new key generation.');
                return;
            }

            console.log('[SignalManager] No keys found. Generating new keys...');

            // Generate identity key pair
            const identityKeyPair = await KeyHelper.generateIdentityKeyPair();
            await this.store.putIdentityKeyPair(identityKeyPair);

            // Generate registration ID
            const registrationId = KeyHelper.generateRegistrationId();
            await this.store.putRegistrationId(registrationId);

            // Generate signed pre key
            const signedPreKeyId = 456;
            const signedPreKey = await KeyHelper.generateSignedPreKey(identityKeyPair, signedPreKeyId);
            await this.store.storeSignedPreKey(signedPreKeyId, signedPreKey.keyPair);

            // Generate one-time pre keys
            const preKeys = [];
            for (let i = 0; i < 100; i++) {
                const preKey = await KeyHelper.generatePreKey(i);
                await this.store.storePreKey(preKey.keyId, preKey.keyPair);
                preKeys.push(preKey);
            }

            // Upload keys to server
            await this.uploadKeys(identityKeyPair, signedPreKey, preKeys);

            console.log('[SignalManager] Initialization complete');
        } catch (error) {
            console.error('[SignalManager] Initialization error:', error);
            throw error;
        }
    }

    private async uploadKeys(
        identityKeyPair: any,
        signedPreKey: any,
        preKeys: any[]
    ): Promise<void> {
        try {
            // Upload identity key
            await axios.post(`${SERVER_URL}/api/keys/identity`, {
                userId: this.userId,
                publicKey: this.arrayBufferToBase64(identityKeyPair.pubKey),
                registrationId: await this.store.getLocalRegistrationId()
            });

            // Upload signed pre key
            await axios.post(`${SERVER_URL}/api/keys/signed-prekey`, {
                userId: this.userId,
                keyId: 456,
                publicKey: this.arrayBufferToBase64(signedPreKey.keyPair.pubKey),
                signature: this.arrayBufferToBase64(signedPreKey.signature)
            });

            // Upload one-time pre keys
            for (const preKey of preKeys) {
                await axios.post(`${SERVER_URL}/api/keys/prekey`, {
                    userId: this.userId,
                    keyId: preKey.keyId,
                    publicKey: this.arrayBufferToBase64(preKey.keyPair.pubKey)
                });
            }

            console.log('[SignalManager] Keys uploaded successfully');
        } catch (error) {
            console.error('[SignalManager] Key upload error:', error);
            throw error;
        }
    }

    async buildSession(recipientId: string): Promise<void> {
        console.log('[SignalManager] Building session with:', recipientId);

        try {
            // Fetch recipient's key bundle
            const response = await axios.get(`${SERVER_URL}/api/keys/bundle/${recipientId}`);
            const bundle = response.data;

            // Create session builder
            const address = new SignalProtocolAddress(recipientId, 1);
            const sessionBuilder = new SessionBuilder(this.store, address);

            // Process pre key bundle
            await sessionBuilder.processPreKey({
                registrationId: bundle.identityKey.registrationId,
                identityKey: this.base64ToArrayBuffer(bundle.identityKey.publicKey),
                signedPreKey: {
                    keyId: bundle.signedPreKey.keyId,
                    publicKey: this.base64ToArrayBuffer(bundle.signedPreKey.publicKey),
                    signature: this.base64ToArrayBuffer(bundle.signedPreKey.signature)
                },
                preKey: bundle.preKey ? {
                    keyId: bundle.preKey.keyId,
                    publicKey: this.base64ToArrayBuffer(bundle.preKey.publicKey)
                } : undefined
            });

            console.log('[SignalManager] Session built successfully');
        } catch (error) {
            console.error('[SignalManager] Session build error:', error);
            throw error;
        }
    }

    async encryptMessage(recipientId: string, message: string): Promise<{ type: number, body: string }> {
        console.log('[SignalManager] Encrypting message for:', recipientId);

        try {
            const address = new SignalProtocolAddress(recipientId, 1);
            const sessionCipher = new SessionCipher(this.store, address);

            // Check if session exists, if not build it
            const sessionStr = await this.store.loadSession(address.toString());
            if (!sessionStr) {
                await this.buildSession(recipientId);
            }

            const plaintext = new TextEncoder().encode(message);
            const ciphertext = await sessionCipher.encrypt(plaintext.buffer as ArrayBuffer);

            return {
                type: ciphertext.type,
                body: typeof ciphertext.body === 'string' ? ciphertext.body : this.arrayBufferToBase64(ciphertext.body as any)
            };
        } catch (error) {
            console.error('[SignalManager] Encryption error:', error);
            throw error;
        }
    }

    async decryptMessage(senderId: string, ciphertext: { type: number, body: string }): Promise<string> {
        console.log('[SignalManager] Decrypting message from:', senderId);

        try {
            const address = new SignalProtocolAddress(senderId, 1);
            const sessionCipher = new SessionCipher(this.store, address);

            // The library accepts base64 string directly
            let plaintext: ArrayBuffer;
            if (ciphertext.type === 3) { // PREKEY_BUNDLE
                plaintext = await sessionCipher.decryptPreKeyWhisperMessage(ciphertext.body);
            } else { // WHISPER_MESSAGE
                plaintext = await sessionCipher.decryptWhisperMessage(ciphertext.body);
            }

            return new TextDecoder().decode(plaintext);
        } catch (error) {
            console.error('[SignalManager] Decryption error:', error);
            throw error;
        }
    }

    // Utility methods
    private arrayBufferToBase64(buffer: ArrayBuffer): string {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }

    private base64ToArrayBuffer(base64: string): ArrayBuffer {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes.buffer;
    }
}
