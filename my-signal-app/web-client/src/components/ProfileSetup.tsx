import { useState, useRef } from 'react';
import {
    Box,
    Container,
    TextField,
    Button,
    Paper,
    Typography,
    Avatar,
    Fade,
    CircularProgress,
    IconButton,
    InputAdornment,
} from '@mui/material';
import {
    Person as PersonIcon,
    Photo as PhotoIcon,
    EmojiEmotions as EmojiIcon,
    Close as CloseIcon,
} from '@mui/icons-material';
import axios from 'axios';
import { SERVER_URL } from '../utils/SignalManager';

interface ProfileSetupProps {
    userId: string;
    onComplete: () => void;
    onError: (message: string) => void;
}

export function ProfileSetup({ userId, onComplete, onError }: ProfileSetupProps) {
    const [displayName, setDisplayName] = useState('');
    const [profilePicture, setProfilePicture] = useState<string | null>(null);
    const [mood, setMood] = useState('');
    const [loading, setLoading] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleImageUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;

        // Validate file type
        if (!file.type.startsWith('image/')) {
            onError('Please select a valid image file (JPG, PNG)');
            return;
        }

        // Validate file size (max 400KB)
        if (file.size > 400 * 1024) {
            onError('Image must be less than 400KB');
            return;
        }

        // Convert to Base64
        const reader = new FileReader();
        reader.onloadend = () => {
            const base64 = reader.result as string;
            setProfilePicture(base64);
        };
        reader.readAsDataURL(file);
    };

    const removeProfilePicture = () => {
        setProfilePicture(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleSubmit = async () => {
        // Validate display name
        if (!displayName.trim()) {
            onError('Display name is required');
            return;
        }

        if (displayName.trim().length < 2) {
            onError('Display name must be at least 2 characters');
            return;
        }

        if (displayName.length > 50) {
            onError('Display name must not exceed 50 characters');
            return;
        }

        setLoading(true);
        try {
            const response = await axios.post(`${SERVER_URL}/api/profile/setup`, {
                userId,
                displayName: displayName.trim(),
                profilePicture: profilePicture || undefined,
                mood: mood.trim() || undefined,
            });

            if (response.data.success) {
                onComplete();
            } else {
                onError(response.data.error || 'Failed to set up profile');
            }
        } catch (error: any) {
            const errorMsg = error.response?.data?.error || 'Failed to set up profile';
            onError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey && displayName.trim()) {
            e.preventDefault();
            handleSubmit();
        }
    };

    return (
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
                                background: 'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                margin: '0 auto 20px',
                                boxShadow: '0 8px 24px rgba(245, 158, 11, 0.3)',
                            }}
                        >
                            <PersonIcon sx={{ fontSize: 40, color: 'white' }} />
                        </Box>
                        <Typography variant="h4" gutterBottom fontWeight="bold" color="primary">
                            Set Up Your Profile
                        </Typography>
                        <Typography variant="body1" color="text.secondary">
                            Let's personalize your Onparl experience
                        </Typography>
                    </Box>

                    {/* Profile Picture Upload */}
                    <Box sx={{ mb: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/*"
                            style={{ display: 'none' }}
                            onChange={handleImageUpload}
                        />

                        <Box sx={{ position: 'relative', mb: 2 }}>
                            <Avatar
                                src={profilePicture || undefined}
                                sx={{
                                    width: 120,
                                    height: 120,
                                    bgcolor: 'primary.main',
                                    fontSize: '3rem',
                                    cursor: 'pointer',
                                    border: '4px solid',
                                    borderColor: profilePicture ? 'primary.main' : 'grey.300',
                                    transition: 'all 0.3s',
                                    '&:hover': {
                                        transform: 'scale(1.05)',
                                        borderColor: 'primary.main',
                                    },
                                }}
                                onClick={() => fileInputRef.current?.click()}
                            >
                                {!profilePicture && <PhotoIcon sx={{ fontSize: 48 }} />}
                            </Avatar>

                            {profilePicture && (
                                <IconButton
                                    size="small"
                                    sx={{
                                        position: 'absolute',
                                        top: 0,
                                        right: 0,
                                        bgcolor: 'error.main',
                                        color: 'white',
                                        '&:hover': { bgcolor: 'error.dark' },
                                    }}
                                    onClick={removeProfilePicture}
                                >
                                    <CloseIcon fontSize="small" />
                                </IconButton>
                            )}
                        </Box>

                        <Button
                            variant="outlined"
                            size="small"
                            startIcon={<PhotoIcon />}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            {profilePicture ? 'Change Photo' : 'Add Photo (Optional)'}
                        </Button>
                        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
                            JPG or PNG, max 400KB
                        </Typography>
                    </Box>

                    {/* Display Name */}
                    <TextField
                        fullWidth
                        required
                        label="Display Name"
                        placeholder="Enter your name"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        onKeyPress={handleKeyPress}
                        disabled={loading}
                        error={displayName.length > 50}
                        helperText={`${displayName.length}/50 characters${displayName.length > 50 ? ' - Too long!' : ''}`}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <PersonIcon color="primary" />
                                </InputAdornment>
                            ),
                        }}
                        sx={{ mb: 3 }}
                    />

                    {/* Mood/Status */}
                    <TextField
                        fullWidth
                        label="Mood (Optional)"
                        placeholder="What's on your mind?"
                        value={mood}
                        onChange={(e) => setMood(e.target.value)}
                        onKeyPress={handleKeyPress}
                        disabled={loading}
                        multiline
                        maxRows={2}
                        error={mood.length > 100}
                        helperText={`${mood.length}/100 characters${mood.length > 100 ? ' - Too long!' : ''}`}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <EmojiIcon color="action" />
                                </InputAdornment>
                            ),
                        }}
                        sx={{ mb: 4 }}
                    />

                    <Button
                        fullWidth
                        variant="contained"
                        size="large"
                        onClick={handleSubmit}
                        disabled={loading || !displayName.trim() || displayName.length > 50 || mood.length > 100}
                        startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <PersonIcon />}
                        sx={{
                            py: 1.5,
                            fontSize: '1.1rem',
                            background: 'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
                            '&:hover': {
                                background: 'linear-gradient(135deg, #d97706 0%, #dc2626 100%)',
                            },
                        }}
                    >
                        {loading ? 'Setting up...' : 'Complete Setup'}
                    </Button>

                    <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                        You can update your profile later from settings
                    </Typography>
                </Paper>
            </Container>
        </Fade>
    );
}
