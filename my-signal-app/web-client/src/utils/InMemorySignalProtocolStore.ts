import type { KeyPairType } from '@privacyresearch/libsignal-protocol-typescript';

export class InMemorySignalProtocolStore {
    private store: Record<string, any> = {};
    private userId: string;

    constructor(userId: string) {
        this.userId = userId;
        this.load();
    }

    // Direction enum for isTrustedIdentity
    Direction = {
        SENDING: 1,
        RECEIVING: 2
    };

    private load() {
        try {
            const saved = localStorage.getItem(`signal_store_${this.userId}`);
            if (saved) {
                const data = JSON.parse(saved);
                this.store = {
                    identityKey: this.deserializeKeyPair(data.identityKey),
                    registrationId: data.registrationId,
                    preKeys: this.deserializeKeyPairs(data.preKeys),
                    signedPreKeys: this.deserializeKeyPairs(data.signedPreKeys),
                    sessions: data.sessions || {},
                    trustedKeys: this.deserializeTrustedKeys(data.trustedKeys)
                };
                console.log('[Store] Loaded existing Signal data');
            }
        } catch (e) {
            console.error('[Store] Failed to load data', e);
            this.store = {};
        }
    }

    private save() {
        try {
            const data = {
                identityKey: this.serializeKeyPair(this.store.identityKey),
                registrationId: this.store.registrationId,
                preKeys: this.serializeKeyPairs(this.store.preKeys),
                signedPreKeys: this.serializeKeyPairs(this.store.signedPreKeys),
                sessions: this.store.sessions,
                trustedKeys: this.serializeTrustedKeys(this.store.trustedKeys)
            };
            localStorage.setItem(`signal_store_${this.userId}`, JSON.stringify(data));
        } catch (e) {
            console.error('[Store] Failed to save data', e);
        }
    }

    async getIdentityKeyPair(): Promise<KeyPairType | undefined> {
        return this.store.identityKey;
    }

    async getLocalRegistrationId(): Promise<number | undefined> {
        return this.store.registrationId;
    }

    async isTrustedIdentity(
        identifier: string,
        identityKey: ArrayBuffer,
        _direction: number
    ): Promise<boolean> {
        if (!this.store.trustedKeys) {
            this.store.trustedKeys = {};
        }
        const trusted = this.store.trustedKeys[identifier];
        if (trusted === undefined) {
            return true; // Trust on first use
        }
        return this.arrayBufferEquals(identityKey, trusted);
    }

    async loadIdentityKey(identifier: string): Promise<ArrayBuffer | undefined> {
        if (!this.store.trustedKeys) {
            return undefined;
        }
        return this.store.trustedKeys[identifier];
    }

    async saveIdentity(identifier: string, identityKey: ArrayBuffer): Promise<boolean> {
        if (!this.store.trustedKeys) {
            this.store.trustedKeys = {};
        }
        const existing = this.store.trustedKeys[identifier];
        this.store.trustedKeys[identifier] = identityKey;
        this.save(); // PERSIST
        return existing !== undefined && !this.arrayBufferEquals(existing, identityKey);
    }

    async loadPreKey(keyId: string | number): Promise<KeyPairType | undefined> {
        if (!this.store.preKeys) {
            return undefined;
        }
        return this.store.preKeys[keyId];
    }

    async storePreKey(keyId: string | number, keyPair: KeyPairType): Promise<void> {
        if (!this.store.preKeys) {
            this.store.preKeys = {};
        }
        this.store.preKeys[keyId] = keyPair;
        this.save(); // PERSIST
    }

    async removePreKey(keyId: string | number): Promise<void> {
        if (this.store.preKeys) {
            delete this.store.preKeys[keyId];
            this.save(); // PERSIST
        }
    }

    async loadSignedPreKey(keyId: string | number): Promise<KeyPairType | undefined> {
        if (!this.store.signedPreKeys) {
            return undefined;
        }
        return this.store.signedPreKeys[keyId];
    }

    async storeSignedPreKey(keyId: string | number, keyPair: KeyPairType): Promise<void> {
        if (!this.store.signedPreKeys) {
            this.store.signedPreKeys = {};
        }
        this.store.signedPreKeys[keyId] = keyPair;
        this.save(); // PERSIST
    }

    async removeSignedPreKey(keyId: string | number): Promise<void> {
        if (this.store.signedPreKeys) {
            delete this.store.signedPreKeys[keyId];
            this.save(); // PERSIST
        }
    }

    async loadSession(identifier: string): Promise<string | undefined> {
        if (!this.store.sessions) {
            return undefined;
        }
        return this.store.sessions[identifier];
    }

    async storeSession(identifier: string, record: string): Promise<void> {
        if (!this.store.sessions) {
            this.store.sessions = {};
        }
        this.store.sessions[identifier] = record;
        this.save(); // PERSIST
    }

    async removeSession(identifier: string): Promise<void> {
        if (this.store.sessions) {
            delete this.store.sessions[identifier];
            this.save(); // PERSIST
        }
    }

    async removeAllSessions(identifier: string): Promise<void> {
        if (this.store.sessions) {
            let changed = false;
            Object.keys(this.store.sessions).forEach(key => {
                if (key.startsWith(identifier)) {
                    delete this.store.sessions[key];
                    changed = true;
                }
            });
            if (changed) this.save(); // PERSIST
        }
    }

    // Helper methods
    async putIdentityKeyPair(keyPair: KeyPairType): Promise<void> {
        this.store.identityKey = keyPair;
        this.save(); // PERSIST
    }

    async putRegistrationId(registrationId: number): Promise<void> {
        this.store.registrationId = registrationId;
        this.save(); // PERSIST
    }

    private arrayBufferEquals(a: ArrayBuffer, b: ArrayBuffer): boolean {
        const ua = new Uint8Array(a);
        const ub = new Uint8Array(b);
        if (ua.length !== ub.length) return false;
        for (let i = 0; i < ua.length; i++) {
            if (ua[i] !== ub[i]) return false;
        }
        return true;
    }

    // Serialization Helpers
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

    private serializeKeyPair(kp?: KeyPairType): any {
        if (!kp) return undefined;
        return {
            pubKey: this.arrayBufferToBase64(kp.pubKey),
            privKey: this.arrayBufferToBase64(kp.privKey)
        };
    }

    private deserializeKeyPair(data?: any): KeyPairType | undefined {
        if (!data) return undefined;
        return {
            pubKey: this.base64ToArrayBuffer(data.pubKey),
            privKey: this.base64ToArrayBuffer(data.privKey)
        };
    }

    private serializeKeyPairs(map?: Record<string | number, KeyPairType>): any {
        if (!map) return undefined;
        const out: any = {};
        // Use for...in for object iteration
        for (const k in map) {
            out[k] = this.serializeKeyPair(map[k]);
        }
        return out;
    }

    private deserializeKeyPairs(map?: any): Record<string | number, KeyPairType> | undefined {
        if (!map) return undefined;
        const out: any = {};
        for (const k in map) {
            out[k] = this.deserializeKeyPair(map[k]);
        }
        return out;
    }

    private serializeTrustedKeys(map?: Record<string, ArrayBuffer>): any {
        if (!map) return undefined;
        const out: any = {};
        for (const k in map) {
            out[k] = this.arrayBufferToBase64(map[k]);
        }
        return out;
    }

    private deserializeTrustedKeys(map?: any): Record<string, ArrayBuffer> | undefined {
        if (!map) return undefined;
        const out: any = {};
        for (const k in map) {
            out[k] = this.base64ToArrayBuffer(map[k]);
        }
        return out;
    }
}

