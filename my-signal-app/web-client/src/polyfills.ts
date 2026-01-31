import { Buffer } from 'buffer';

// Polyfill global
if (typeof (window as any).global === 'undefined') {
    (window as any).global = window;
}

// Polyfill Buffer
if (typeof (window as any).Buffer === 'undefined') {
    (window as any).Buffer = Buffer;
}

// Polyfill process
if (typeof (window as any).process === 'undefined') {
    (window as any).process = { env: {} };
}
