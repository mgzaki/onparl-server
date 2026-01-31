import type { KeyPairType } from '@privacyresearch/libsignal-protocol-typescript';

export class InMemorySignalProtocolStore {
    private store: Record<string, any> = {};

    // Direction enum for isTrustedIdentity
    Direction = {
        SENDING: 1,
        RECEIVING: 2
    };

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
    }

    async removePreKey(keyId: string | number): Promise<void> {
        if (this.store.preKeys) {
            delete this.store.preKeys[keyId];
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
    }

    async removeSignedPreKey(keyId: string | number): Promise<void> {
        if (this.store.signedPreKeys) {
            delete this.store.signedPreKeys[keyId];
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
    }

    async removeSession(identifier: string): Promise<void> {
        if (this.store.sessions) {
            delete this.store.sessions[identifier];
        }
    }

    async removeAllSessions(identifier: string): Promise<void> {
        if (this.store.sessions) {
            Object.keys(this.store.sessions).forEach(key => {
                if (key.startsWith(identifier)) {
                    delete this.store.sessions[key];
                }
            });
        }
    }

    // Helper methods
    async putIdentityKeyPair(keyPair: KeyPairType): Promise<void> {
        this.store.identityKey = keyPair;
    }

    async putRegistrationId(registrationId: number): Promise<void> {
        this.store.registrationId = registrationId;
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
}
