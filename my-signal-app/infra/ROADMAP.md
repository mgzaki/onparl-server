# Onparl → WhatsApp Clone Roadmap

## ✅ COMPLETED (Current Features)
- [x] End-to-end encryption (Signal Protocol)
- [x] Real-time messaging via WebSocket
- [x] User identification
- [x] Basic message sending/receiving
- [x] AWS infrastructure deployment

---

## 🔴 CRITICAL - Core Messaging Features

### User Management
- [ ] Phone number-based authentication (OTP/SMS verification)
- [ ] User profiles (name, about, profile picture)
- [ ] Contact synchronization from phone
- [ ] Contact management (add, remove, block)
- [ ] User search by phone number/name

### Essential Messaging
- [ ] Message persistence (save chat history to DB)
- [ ] Message delivery status (sent ✓, delivered ✓✓, read ✓✓)
- [ ] Typing indicators ("Alice is typing...")
- [ ] Online/offline/last seen status
- [ ] Message timestamps with proper timezone handling
- [ ] Unread message counter per chat
- [ ] Message order guarantee and deduplication

### Group Chats
- [ ] Create group (name, icon)
- [ ] Add/remove participants
- [ ] Group admin permissions
- [ ] Group info & settings
- [ ] Exit group functionality
- [ ] Group participant list with roles

---

## 🟠 HIGH PRIORITY - Enhanced Messaging

### Rich Media Support
- [ ] Image sharing (compress, preview, full-size view)
- [ ] Video sharing (compress, preview, playback)
- [ ] Document sharing (PDF, docs, etc.)
- [ ] Voice messages (record, play, waveform visualization)
- [ ] Audio file sharing
- [ ] Camera integration (take photo/video directly)

### Message Interactions
- [ ] Reply to specific messages (threading)
- [ ] Forward messages to other chats
- [ ] Delete messages (delete for me / delete for everyone)
- [ ] Copy message text
- [ ] Star/favorite important messages
- [ ] Message search (global and per-chat)
- [ ] Emoji reactions to messages

### Chat Management
- [ ] Archive chats
- [ ] Mute notifications (per chat)
- [ ] Pin important chats to top
- [ ] Clear chat history
- [ ] Delete entire chat
- [ ] Export chat history

---

## 🟡 MEDIUM PRIORITY - Communication Features

### Voice & Video Calling
- [ ] One-on-one voice calls (WebRTC)
- [ ] One-on-one video calls (WebRTC)
- [ ] Group voice calls
- [ ] Group video calls
- [ ] Call history log
- [ ] Call quality indicators
- [ ] Mute/unmute during calls
- [ ] Screen sharing

### Push Notifications
- [ ] Mobile push notifications (FCM/APNS)
- [ ] Desktop notifications
- [ ] Notification customization per chat
- [ ] Preview message content in notifications
- [ ] Quick reply from notifications

### Status/Stories
- [ ] Post text status
- [ ] Post image/video status
- [ ] View others' status
- [ ] Status privacy settings (who can view)
- [ ] Status 24-hour auto-delete
- [ ] Status view receipts

---

## 🟢 NICE-TO-HAVE - User Experience

### UI/UX Enhancements
- [ ] Dark mode / Light mode / System theme
- [ ] Custom chat wallpapers
- [ ] Font size settings
- [ ] Chat color themes
- [ ] Accessibility features (screen reader support)
- [ ] Keyboard shortcuts
- [ ] Drag & drop media sharing
- [ ] Link previews (auto-extract title, image, description)

### Stickers & Expressions
- [ ] Sticker packs (download, send, manage)
- [ ] GIF search and sharing
- [ ] Custom emoji skin tones
- [ ] Animated emojis

### Advanced Features
- [ ] Polls in chats
- [ ] Location sharing (static map)
- [ ] Live location sharing (real-time tracking)
- [ ] Contact card sharing
- [ ] Disappearing messages (auto-delete after time)
- [ ] Message scheduling (send later)
- [ ] Voice message playback speed control

---

## 🔵 ADVANCED - Enterprise & Multi-Platform

### Multi-Device Support
- [ ] Web client (already have basic version)
- [ ] Desktop app (Electron)
- [ ] Tablet optimization
- [ ] Device linking (QR code scan)
- [ ] Sync messages across devices
- [ ] Device management (see all active sessions)
- [ ] Remote logout from other devices

### Data & Privacy
- [ ] End-to-end encrypted backups
- [ ] Cloud backup to AWS S3
- [ ] Backup restore functionality
- [ ] Two-factor authentication (2FA)
- [ ] Fingerprint/Face ID lock
- [ ] Privacy settings (last seen, profile photo, about, status)
- [ ] Security notifications (encryption changes)
- [ ] Verified accounts (checkmark badges)

### Business Features
- [ ] Broadcast lists (send to multiple contacts)
- [ ] Business accounts
- [ ] Catalogs (product listings)
- [ ] Quick replies (saved message templates)
- [ ] Auto-reply messages
- [ ] Labels/tags for chat organization
- [ ] Communities (multi-group management like WhatsApp)

---

## 🏗️ INFRASTRUCTURE & PERFORMANCE

### Backend Improvements
- [ ] Message queue for reliability (SQS)
- [ ] Redis caching for online status
- [ ] CDN for media files (CloudFront)
- [ ] Database indexing optimization
- [ ] **Horizontal scaling for ECS**
  - [ ] Add ECS Service Auto Scaling (CPU, memory, request-based)
  - [ ] Configure min capacity: 2, max capacity: 10
  - [ ] Set target tracking policies (CPU: 75%, Memory: 80%)
  - [ ] Implement scale-in/scale-out cooldown periods
  - [ ] Update desired_count from 1 to 2 for HA
- [ ] **WebSocket Scaling Architecture**
  - [ ] Add Redis/ElastiCache for message broker (STOMP over Redis)
  - [ ] Configure Spring Session with Redis backend
  - [ ] Implement distributed WebSocket message routing
  - [ ] Test multi-instance WebSocket message delivery
  - [ ] Alternative: Evaluate Amazon MQ or AWS IoT Core
- [ ] Rate limiting & API throttling
- [ ] **Monitoring & logging (CloudWatch, X-Ray)**
  - [ ] CloudWatch alarms for autoscaling triggers
  - [ ] Custom metrics for WebSocket connection count
  - [ ] CloudWatch dashboard for scaling visibility
  - [ ] AWS X-Ray distributed tracing
  - [ ] Application performance monitoring (APM)
- [ ] **Load Testing & Capacity Planning**
  - [ ] Load test scenarios for concurrent WebSocket connections
  - [ ] Validate autoscaling behavior under stress
  - [ ] Document scaling thresholds and limits
  - [ ] Performance benchmarking (latency, throughput)
- [ ] Automated testing (unit, integration, E2E)

### Security & Compliance
- [ ] GDPR compliance (data export, deletion)
- [ ] Content moderation (AI-based)
- [ ] Spam detection
- [ ] Report & block abuse
- [ ] Audit logs
- [ ] Penetration testing
- [ ] Security headers & CORS hardening

---

## 📊 PRIORITY SUMMARY

**Phase 1 (MVP+):** Critical items - User auth, profiles, message persistence, delivery receipts, typing indicators, group chats, basic media sharing

**Phase 2 (Core Product):** High priority - Rich media, message interactions, voice/video calls, push notifications

**Phase 3 (Competitive):** Medium priority - Status/Stories, UI polish, advanced messaging features

**Phase 4 (Premium):** Nice-to-have - Stickers, polls, location sharing, disappearing messages

**Phase 5 (Enterprise):** Advanced - Multi-device, backups, business features, communities

---

## 🎯 RECOMMENDED NEXT STEPS (Phase 1)

1. **User Authentication System**
   - Phone number verification with Twilio/AWS SNS
   - User profile creation & management
   - JWT/session-based auth

2. **Message Persistence & History**
   - Update DynamoDB schema for chat history
   - Implement pagination for message loading
   - Message sync on reconnection

3. **Delivery & Read Receipts**
   - Track message states (sent, delivered, read)
   - Display status indicators in UI
   - Update via WebSocket events

4. **Typing Indicators & Presence**
   - Send typing events
   - Show "typing..." in chat
   - Implement online/offline/last seen

5. **Group Chat Foundation**
   - Group creation & management APIs
   - Multi-recipient message handling
   - Group membership tracking
