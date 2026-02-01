import { useState, useEffect, useRef } from 'react';
import {
  Box,
  Container,
  TextField,
  Button,
  Paper,
  Typography,
  Avatar,
  AppBar,
  Toolbar,
  Chip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Fade,
  CircularProgress,
  Snackbar,
  Alert,
  Stack,
  InputAdornment,
  LinearProgress,
} from '@mui/material';
import {
  Send as SendIcon,
  Lock as LockIcon,
  ExpandMore as ExpandMoreIcon,
  BugReport as BugReportIcon,
  Person as PersonIcon,
  Chat as ChatIcon,
  Phone as PhoneIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { SignalManager, SERVER_URL } from './utils/SignalManager';
import { ProfileSetup } from './components/ProfileSetup';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

interface Message {
  senderId: string;
  content: string;
  timestamp: number;
}

type AuthStep = 'phone' | 'otp' | 'profile' | 'authenticated';

function App() {
  // Authentication state
  const [authStep, setAuthStep] = useState<AuthStep>('phone');
  const [authType, setAuthType] = useState<'PHONE' | 'EMAIL'>('PHONE');
  const [identifier, setIdentifier] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [userId, setUserId] = useState('');
  const [isNewUser, setIsNewUser] = useState(false);

  // Chat state
  const [manager, setManager] = useState<SignalManager | null>(null);
  const [recipientId, setRecipientId] = useState('');
  const [messageText, setMessageText] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [logs, setLogs] = useState<string[]>([]);

  // UI state
  const [loading, setLoading] = useState(false);
  const [otpCountdown, setOtpCountdown] = useState(0);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' | 'info' }>({
    open: false,
    message: '',
    severity: 'success',
  });

  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const otpInputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const log = (msg: string) => setLogs((prev) => [...prev, `[${new Date().toLocaleTimeString()}] ${msg}`]);

  const showSnackbar = (message: string, severity: 'success' | 'error' | 'info') => {
    setSnackbar({ open: true, message, severity });
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // OTP countdown timer
  useEffect(() => {
    if (otpCountdown > 0) {
      const timer = setTimeout(() => setOtpCountdown(otpCountdown - 1), 1000);
      return () => clearTimeout(timer);
    }
  }, [otpCountdown]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, []);

  /**
   * REQUEST OTP - Step 1: Send OTP to identifier
   */
  const requestOtp = async () => {
    if (!identifier.trim()) {
      showSnackbar(`Please enter a valid ${authType === 'PHONE' ? 'phone number' : 'email'}`, 'error');
      return;
    }

    if (authType === 'PHONE' && !identifier.startsWith('+')) {
      showSnackbar('Phone number must start with + and country code (e.g., +1)', 'error');
      return;
    }

    if (authType === 'EMAIL' && !identifier.includes('@')) {
      showSnackbar('Please enter a valid email address', 'error');
      return;
    }

    setLoading(true);
    try {
      const response = await axios.post(`${SERVER_URL}/api/auth/request-otp`, {
        identifier: identifier.trim(),
        type: authType
      });

      if (response.data.success) {
        setAuthStep('otp');
        setOtpCountdown(300); // 5 minutes
        showSnackbar(response.data.message || 'OTP sent successfully!', 'success');
        log('OTP requested successfully');

        // Auto-focus first OTP input
        setTimeout(() => otpInputRefs.current[0]?.focus(), 300);
      }
    } catch (error: any) {
      const errorMsg = error.response?.data?.error || 'Failed to send OTP';
      showSnackbar(errorMsg, 'error');
      log(`OTP request failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  /**
   * DEV LOGIN - Bypass OTP
   */
  const handleDevLogin = async () => {
    if (!identifier.trim()) {
      showSnackbar('Please enter an identifier', 'error');
      return;
    }

    setLoading(true);
    try {
      const response = await axios.post(`${SERVER_URL}/api/auth/dev-login`, {
        identifier: identifier.trim(),
        type: identifier.includes('@') ? 'EMAIL' : 'PHONE'
      });

      if (response.data.success) {
        setUserId(response.data.userId);
        setIsNewUser(response.data.isNewUser);
        showSnackbar('Dev Login successful!', 'success');
        log(`Dev Authenticated as ${response.data.userId}`);

        if (!response.data.profileComplete) {
          setAuthStep('profile');
        } else {
          await initializeSignal(response.data.userId);
        }
      }
    } catch (error: any) {
      const errorMsg = error.response?.data?.error || 'Dev Login failed';
      showSnackbar(errorMsg, 'error');
      log(`Dev Login failed: ${errorMsg}`);
    } finally {
      setLoading(false);
    }
  };

  /**
   * VERIFY OTP - Step 2: Verify OTP and authenticate
   */
  const verifyOtp = async () => {
    const otpCode = otp.join('');

    if (otpCode.length !== 6) {
      showSnackbar('Please enter the complete 6-digit code', 'error');
      return;
    }

    setLoading(true);
    try {
      const response = await axios.post(`${SERVER_URL}/api/auth/verify-otp`, {
        identifier: identifier.trim(),
        otp: otpCode,
        type: authType
      });

      if (response.data.success) {
        setUserId(response.data.userId);
        setIsNewUser(response.data.isNewUser);
        showSnackbar('Authentication successful!', 'success');
        log(`Authenticated as ${response.data.userId}`);

        // Check if profile setup is needed
        if (!response.data.profileComplete) {
          setAuthStep('profile');
          log('Profile setup required');
        } else {
          // Initialize Signal Protocol
          await initializeSignal(response.data.userId);
        }
      }
    } catch (error: any) {
      const errorMsg = error.response?.data?.error || 'Invalid OTP';
      showSnackbar(errorMsg, 'error');
      log(`OTP verification failed: ${errorMsg}`);

      // Clear OTP inputs on error
      setOtp(['', '', '', '', '', '']);
      otpInputRefs.current[0]?.focus();
    } finally {
      setLoading(false);
    }
  };

  /**
   * Initialize Signal Protocol after authentication
   */
  const initializeSignal = async (authenticatedUserId: string) => {
    try {
      setLoading(true);
      const mgr = new SignalManager(authenticatedUserId);
      await mgr.initialize();
      setManager(mgr);
      log(`Signal Protocol initialized for ${authenticatedUserId}`);

      // Connect WebSocket
      connectWebSocket(authenticatedUserId, mgr);

      setAuthStep('authenticated');
    } catch (error: any) {
      log(`Signal initialization failed: ${error.message}`);
      showSnackbar('Failed to initialize secure messaging', 'error');
    } finally {
      setLoading(false);
    }
  };

  const connectWebSocket = (uId: string, mgr: SignalManager) => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${SERVER_URL}/ws-signal`),
      onConnect: () => {
        log('WebSocket Connected');
        showSnackbar('Connected to server', 'success');

        client.subscribe(`/topic/messages/${uId}`, async (message) => {
          if (message.body) {
            const msgData = JSON.parse(message.body);
            log(`Received message from ${msgData.senderId}`);
            await decryptAndAddMessage(msgData, mgr);
          }
        });
      },
      onStompError: (frame) => {
        log(`Broker reported error: ${frame.headers['message']}`);
        console.error(frame.body);
        showSnackbar('Connection error', 'error');
      },
    });

    client.activate();
    stompClientRef.current = client;
  };

  const decryptAndAddMessage = async (msg: any, mgr: SignalManager) => {
    try {
      const ciphertext = JSON.parse(msg.content);
      const plaintext = await mgr.decryptMessage(msg.senderId, ciphertext);

      setMessages((prev) => [
        ...prev,
        {
          senderId: msg.senderId,
          content: plaintext,
          timestamp: msg.timestamp,
        },
      ]);
      log(`Decrypted message from ${msg.senderId}`);
    } catch (e) {
      console.error('Decryption failed', e);
      log(`Failed to decrypt message from ${msg.senderId}`);
      showSnackbar('Failed to decrypt message', 'error');
    }
  };

  const sendMessage = async () => {
    if (!manager || !recipientId.trim() || !messageText.trim()) {
      showSnackbar('Please enter recipient ID and message', 'error');
      return;
    }

    try {
      log(`Encrypting message for ${recipientId}...`);
      const ciphertext = await manager.encryptMessage(recipientId, messageText);
      const payload = JSON.stringify(ciphertext);

      await axios.post(`${SERVER_URL}/api/messages`, {
        senderId: userId,
        recipientId: recipientId,
        content: payload,
        timestamp: Date.now(),
      });

      setMessages((prev) => [
        ...prev,
        {
          senderId: userId,
          content: messageText,
          timestamp: Date.now(),
        },
      ]);

      log(`Message sent to ${recipientId}`);
      setMessageText('');
    } catch (e: any) {
      log(`Error sending: ${e.message}`);
      showSnackbar('Failed to send message', 'error');
    }
  };

  /**
   * Handle OTP input changes
   */
  const handleOtpChange = (index: number, value: string) => {
    // Only allow digits
    const digit = value.replace(/[^0-9]/g, '');

    if (digit.length > 1) {
      // Handle paste
      const digits = digit.split('').slice(0, 6);
      const newOtp = [...otp];
      digits.forEach((d, i) => {
        if (index + i < 6) {
          newOtp[index + i] = d;
        }
      });
      setOtp(newOtp);

      // Focus last filled input
      const lastIndex = Math.min(index + digits.length, 5);
      otpInputRefs.current[lastIndex]?.focus();
    } else {
      const newOtp = [...otp];
      newOtp[index] = digit;
      setOtp(newOtp);

      // Auto-focus next input
      if (digit && index < 5) {
        otpInputRefs.current[index + 1]?.focus();
      }
    }
  };

  /**
   * Handle OTP input key events
   */
  const handleOtpKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpInputRefs.current[index - 1]?.focus();
    } else if (e.key === 'Enter' && otp.every(d => d)) {
      verifyOtp();
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (authStep === 'phone') {
        requestOtp();
      } else if (authStep === 'authenticated') {
        sendMessage();
      }
    }
  };

  const getAvatarColor = (id: string) => {
    const colors = ['#2563eb', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4'];
    const index = id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length;
    return colors[index];
  };

  const formatCountdown = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <Box sx={{ minHeight: '100vh', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }}>
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>

      {/* PHONE INPUT SCREEN */}
      {authStep === 'phone' && (
        <Fade in timeout={600}>
          <Container maxWidth="sm" sx={{ pt: 10 }}>
            <Paper
              elevation={3}
              sx={{
                p: 5,
                borderRadius: 4,
                textAlign: 'center',
                background: 'rgba(255, 255, 255, 0.95)',
                backdropFilter: 'blur(10px)',
              }}
            >
              <Box sx={{ mb: 4 }}>
                <Box
                  sx={{
                    width: 80,
                    height: 80,
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 20px',
                    boxShadow: '0 8px 24px rgba(102, 126, 234, 0.3)',
                  }}
                >
                  {authType === 'PHONE' ? <PhoneIcon sx={{ fontSize: 40, color: 'white' }} /> : <ChatIcon sx={{ fontSize: 40, color: 'white' }} />}
                </Box>
                <Typography variant="h3" gutterBottom fontWeight="bold" color="primary">
                  Onparl Chat
                </Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 1 }}>
                  Secure Authentication
                </Typography>
              </Box>

              <Stack direction="row" spacing={2} sx={{ mb: 3 }} justifyContent="center">
                <Button
                  variant={authType === 'PHONE' ? 'contained' : 'outlined'}
                  onClick={() => { setAuthType('PHONE'); setIdentifier(''); }}
                >
                  Phone
                </Button>
                <Button
                  variant={authType === 'EMAIL' ? 'contained' : 'outlined'}
                  onClick={() => { setAuthType('EMAIL'); setIdentifier(''); }}
                >
                  Email
                </Button>
                <Button
                  variant="outlined"
                  color="warning"
                  onClick={() => { setAuthType('DEV' as any); setIdentifier(''); }}
                  sx={{ borderColor: '#f59e0b', color: '#f59e0b', '&:hover': { borderColor: '#d97706', bgcolor: 'rgba(245, 158, 11, 0.04)' } }}
                >
                  Dev / Test
                </Button>
              </Stack>

              <TextField
                fullWidth
                label={authType === 'PHONE' ? "Phone Number" : authType === 'EMAIL' ? "Email Address" : "Any Identifier (Dev Mode)"}
                placeholder={authType === 'PHONE' ? "+1 234 567 8900" : authType === 'EMAIL' ? "user@example.com" : "test-user-1"}
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                onKeyPress={e => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    if ((authType as any) === 'DEV') {
                      handleDevLogin();
                    } else {
                      requestOtp();
                    }
                  }
                }}
                disabled={loading}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      {authType === 'PHONE' ? <PhoneIcon color="primary" /> : authType === 'EMAIL' ? <PersonIcon color="primary" /> : <BugReportIcon color="warning" />}
                    </InputAdornment>
                  ),
                }}
                helperText={authType === 'PHONE' ? "Enter phone with country code (e.g., +1)" : "Enter your email address"}
                sx={{ mb: 3 }}
              />

              <Button
                fullWidth
                variant="contained"
                size="large"
                onClick={(authType as any) === 'DEV' ? handleDevLogin : requestOtp}
                disabled={loading || !identifier.trim()}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <LockIcon />}
                sx={{
                  py: 1.5,
                  fontSize: '1.1rem',
                  background: (authType as any) === 'DEV' ? 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)' : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  '&:hover': {
                    background: (authType as any) === 'DEV' ? 'linear-gradient(135deg, #d97706 0%, #b45309 100%)' : 'linear-gradient(135deg, #5568d3 0%, #6a4292 100%)',
                  },
                }}
              >
                {loading ? 'Processing...' : (authType as any) === 'DEV' ? 'Dev Login (Bypass One Time Password)' : 'Send Verification Code'}
              </Button>
            </Paper>
          </Container>
        </Fade>
      )}

      {/* OTP INPUT SCREEN */}
      {authStep === 'otp' && (
        <Fade in timeout={600}>
          <Container maxWidth="sm" sx={{ pt: 10 }}>
            <Paper
              elevation={3}
              sx={{
                p: 5,
                borderRadius: 4,
                textAlign: 'center',
                background: 'rgba(255, 255, 255, 0.95)',
                backdropFilter: 'blur(10px)',
              }}
            >
              <Box sx={{ mb: 4 }}>
                <Box
                  sx={{
                    width: 80,
                    height: 80,
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    margin: '0 auto 20px',
                    boxShadow: '0 8px 24px rgba(16, 185, 129, 0.3)',
                  }}
                >
                  <LockIcon sx={{ fontSize: 40, color: 'white' }} />
                </Box>
                <Typography variant="h4" gutterBottom fontWeight="bold" color="primary">
                  Enter Verification Code
                </Typography>
                <Typography variant="body1" color="text.secondary" sx={{ mb: 2 }}>
                  We sent a 6-digit code to
                </Typography>
                <Typography variant="h6" color="text.primary" fontWeight="600">
                  {identifier}
                </Typography>
              </Box>

              {/* OTP Input Boxes */}
              <Stack direction="row" spacing={1.5} justifyContent="center" sx={{ mb: 3 }}>
                {otp.map((digit, index) => (
                  <TextField
                    key={index}
                    inputRef={(el) => (otpInputRefs.current[index] = el)}
                    value={digit}
                    onChange={(e) => handleOtpChange(index, e.target.value)}
                    onKeyDown={(e) => handleOtpKeyDown(index, e)}
                    inputProps={{
                      maxLength: 1,
                      style: { textAlign: 'center', fontSize: '1.5rem', fontWeight: 'bold' },
                    }}
                    sx={{
                      width: 56,
                      '& .MuiOutlinedInput-root': {
                        height: 56,
                      },
                    }}
                  />
                ))}
              </Stack>

              {/* Countdown Timer */}
              {otpCountdown > 0 && (
                <Box sx={{ mb: 3 }}>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Code expires in {formatCountdown(otpCountdown)}
                  </Typography>
                  <LinearProgress
                    variant="determinate"
                    value={(otpCountdown / 300) * 100}
                    sx={{ borderRadius: 2 }}
                  />
                </Box>
              )}

              <Button
                fullWidth
                variant="contained"
                size="large"
                onClick={verifyOtp}
                disabled={loading || otp.some(d => !d)}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <LockIcon />}
                sx={{
                  py: 1.5,
                  fontSize: '1.1rem',
                  mb: 2,
                  background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                  '&:hover': {
                    background: 'linear-gradient(135deg, #0ea572 0%, #047857 100%)',
                  },
                }}
              >
                {loading ? 'Verifying...' : 'Verify Code'}
              </Button>

              <Button
                fullWidth
                variant="text"
                onClick={() => {
                  setAuthStep('phone');
                  setOtp(['', '', '', '', '', '']);
                  setIdentifier('');
                }}
                startIcon={<RefreshIcon />}
              >
                Use Different Number
              </Button>

              <Button
                fullWidth
                variant="text"
                onClick={requestOtp}
                disabled={loading || otpCountdown > 240}
                sx={{ mt: 1 }}
              >
                Resend Code {otpCountdown > 240 && `(wait ${formatCountdown(otpCountdown - 240)})`}
              </Button>
            </Paper>
          </Container>
        </Fade>
      )}

      {/* PROFILE SETUP SCREEN */}
      {authStep === 'profile' && (
        <ProfileSetup
          userId={userId}
          onComplete={() => {
            showSnackbar('Profile set up successfully!', 'success');
            log('Profile setup completed');
            initializeSignal(userId);
          }}
          onError={(message) => {
            showSnackbar(message, 'error');
            log(`Profile setup error: ${message}`);
          }}
        />
      )}

      {/* CHAT INTERFACE (unchanged) */}
      {authStep === 'authenticated' && (
        <Fade in timeout={600}>
          <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
            <AppBar position="static" elevation={0} sx={{ background: 'rgba(30, 41, 59, 0.9)', backdropFilter: 'blur(10px)' }}>
              <Toolbar>
                <ChatIcon sx={{ mr: 2 }} />
                <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 600 }}>
                  Onparl Chat {isNewUser && '(New User)'}
                </Typography>
                <Chip
                  avatar={<Avatar sx={{ bgcolor: getAvatarColor(userId), width: 28, height: 28 }}>{userId[0]?.toUpperCase()}</Avatar>}
                  label={userId}
                  variant="outlined"
                  sx={{ color: 'white', borderColor: 'rgba(255, 255, 255, 0.3)', fontWeight: 600 }}
                />
              </Toolbar>
            </AppBar>

            <Container maxWidth="lg" sx={{ flexGrow: 1, py: 3, display: 'flex', flexDirection: 'column' }}>
              <Paper elevation={2} sx={{ p: 2, mb: 2, borderRadius: 3 }}>
                <TextField
                  fullWidth
                  label="Recipient User ID"
                  placeholder="Enter recipient's user ID to chat"
                  value={recipientId}
                  onChange={(e) => setRecipientId(e.target.value)}
                  size="small"
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <PersonIcon color="action" />
                      </InputAdornment>
                    ),
                  }}
                />
              </Paper>

              <Paper
                elevation={2}
                sx={{
                  flexGrow: 1,
                  p: 3,
                  mb: 2,
                  borderRadius: 3,
                  overflowY: 'auto',
                  background: 'linear-gradient(to bottom, #f8fafc 0%, #e2e8f0 100%)',
                  display: 'flex',
                  flexDirection: 'column',
                  minHeight: 0,
                }}
              >
                {messages.length === 0 ? (
                  <Box
                    sx={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      height: '100%',
                      opacity: 0.5,
                    }}
                  >
                    <ChatIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                    <Typography variant="h6" color="text.secondary">
                      No messages yet
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Start a conversation by sending a message
                    </Typography>
                  </Box>
                ) : (
                  <Stack spacing={2}>
                    {messages.map((m, i) => {
                      const isSent = m.senderId === userId;
                      return (
                        <Fade in key={i} timeout={400}>
                          <Box
                            sx={{
                              display: 'flex',
                              alignItems: 'flex-start',
                              justifyContent: isSent ? 'flex-end' : 'flex-start',
                            }}
                          >
                            {!isSent && (
                              <Avatar
                                sx={{
                                  bgcolor: getAvatarColor(m.senderId),
                                  mr: 1.5,
                                  width: 36,
                                  height: 36,
                                }}
                              >
                                {m.senderId[0]?.toUpperCase()}
                              </Avatar>
                            )}
                            <Box sx={{ maxWidth: '70%' }}>
                              <Paper
                                elevation={1}
                                sx={{
                                  p: 2,
                                  borderRadius: 3,
                                  ...(isSent
                                    ? {
                                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                                      color: 'white',
                                      borderBottomRightRadius: 4,
                                    }
                                    : {
                                      bgcolor: 'white',
                                      borderBottomLeftRadius: 4,
                                    }),
                                }}
                              >
                                {!isSent && (
                                  <Typography variant="caption" fontWeight="bold" color="primary" display="block" sx={{ mb: 0.5 }}>
                                    {m.senderId}
                                  </Typography>
                                )}
                                <Typography variant="body1" sx={{ wordBreak: 'break-word' }}>
                                  {m.content}
                                </Typography>
                                <Typography
                                  variant="caption"
                                  sx={{
                                    display: 'block',
                                    mt: 0.5,
                                    opacity: 0.7,
                                    textAlign: 'right',
                                  }}
                                >
                                  {new Date(m.timestamp).toLocaleTimeString()}
                                </Typography>
                              </Paper>
                            </Box>
                            {isSent && (
                              <Avatar
                                sx={{
                                  bgcolor: getAvatarColor(m.senderId),
                                  ml: 1.5,
                                  width: 36,
                                  height: 36,
                                }}
                              >
                                {m.senderId[0]?.toUpperCase()}
                              </Avatar>
                            )}
                          </Box>
                        </Fade>
                      );
                    })}
                    <div ref={messagesEndRef} />
                  </Stack>
                )}
              </Paper>

              <Paper elevation={2} sx={{ p: 2, borderRadius: 3 }}>
                <Stack direction="row" spacing={1.5} alignItems="flex-end">
                  <TextField
                    fullWidth
                    multiline
                    maxRows={4}
                    placeholder="Type your encrypted message..."
                    value={messageText}
                    onChange={(e) => setMessageText(e.target.value)}
                    onKeyPress={handleKeyPress}
                    variant="outlined"
                    sx={{
                      '& .MuiOutlinedInput-root': {
                        borderRadius: 3,
                      },
                    }}
                  />
                  <Button
                    variant="contained"
                    onClick={sendMessage}
                    disabled={!recipientId.trim() || !messageText.trim()}
                    sx={{
                      minWidth: 56,
                      height: 56,
                      borderRadius: 3,
                      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                      '&:hover': {
                        background: 'linear-gradient(135deg, #5568d3 0%, #6a4292 100%)',
                      },
                    }}
                  >
                    <SendIcon />
                  </Button>
                </Stack>
              </Paper>

              <Accordion sx={{ mt: 2, borderRadius: 2 }} elevation={2}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <BugReportIcon sx={{ mr: 1.5, color: 'text.secondary' }} />
                  <Typography fontWeight={600}>Debug Logs ({logs.length})</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Box
                    sx={{
                      bgcolor: '#1e293b',
                      color: '#10b981',
                      p: 2,
                      borderRadius: 2,
                      fontFamily: 'monospace',
                      fontSize: '0.85rem',
                      maxHeight: 200,
                      overflowY: 'auto',
                    }}
                  >
                    {logs.length === 0 ? (
                      <Typography sx={{ opacity: 0.5, fontFamily: 'monospace' }}>No logs yet...</Typography>
                    ) : (
                      logs.map((l, i) => <div key={i}>{l}</div>)
                    )}
                  </Box>
                </AccordionDetails>
              </Accordion>
            </Container>
          </Box>
        </Fade>
      )}
    </Box>
  );
}

export default App;
