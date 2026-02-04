import { useState, useEffect } from 'react';
import {
    Box,
    Paper,
    Typography,
    Avatar,
    List,
    ListItem,
    ListItemButton,
    ListItemAvatar,
    ListItemText,
    ListItemSecondaryAction,
    IconButton,
    Button,
    TextField,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Tabs,
    Tab,
    Menu,
    MenuItem,
    ListItemIcon,
    CircularProgress,
} from '@mui/material';
import {
    PersonAdd as PersonAddIcon,
    Block as BlockIcon,
    Delete as DeleteIcon,
    MoreVert as MoreVertIcon,
    Person as PersonIcon,
} from '@mui/icons-material';
import axios from 'axios';
import { SERVER_URL } from '../utils/SignalManager';

interface Contact {
    userId: string;
    displayName: string;
    profilePicture?: string;
    mood?: string;
    phoneNumber?: string;
}

interface ContactListProps {
    userId: string;
    onSelectContact: (contactId: string) => void;
    currentRecipientId: string;
    onError: (msg: string) => void;
}

export function ContactList({ userId, onSelectContact, currentRecipientId, onError }: ContactListProps) {
    const [contacts, setContacts] = useState<Contact[]>([]);
    const [blockedUsers, setBlockedUsers] = useState<Contact[]>([]);
    const [loading, setLoading] = useState(false);
    const [tabValue, setTabValue] = useState(0);

    // Dialogs
    const [addContactOpen, setAddContactOpen] = useState(false);
    const [newContactId, setNewContactId] = useState('');
    const [addingContact, setAddingContact] = useState(false);

    // Menu
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const [menuContact, setMenuContact] = useState<Contact | null>(null);

    useEffect(() => {
        fetchContacts();
        if (tabValue === 1) {
            fetchBlockedUsers();
        }
    }, [userId, tabValue]);

    const fetchContacts = async () => {
        try {
            const response = await axios.get(`${SERVER_URL}/api/users/${userId}/contacts`);
            if (response.data.success) {
                setContacts(response.data.contacts);
            }
        } catch (error) {
            console.error('Failed to fetch contacts', error);
        }
    };

    const fetchBlockedUsers = async () => {
        setLoading(true);
        try {
            const response = await axios.get(`${SERVER_URL}/api/users/${userId}/blocked`);
            if (response.data.success) {
                setBlockedUsers(response.data.blockedUsers);
            }
        } catch (error) {
            console.error('Failed to fetch blocked users', error);
        } finally {
            setLoading(false);
        }
    };

    const handleAddContact = async () => {
        if (!newContactId.trim()) return;

        setAddingContact(true);
        try {
            const response = await axios.post(`${SERVER_URL}/api/users/${userId}/contacts`, {
                contactIdentifier: newContactId.trim()
            });

            if (response.data.success) {
                setContacts(prev => [...prev, response.data.contact]);
                setAddContactOpen(false);
                setNewContactId('');
                onSelectContact(response.data.contact.userId);
            }
        } catch (error: any) {
            const msg = error.response?.data?.error || 'Failed to add contact';
            onError(msg);
        } finally {
            setAddingContact(false);
        }
    };

    const handleRemoveContact = async (contactId: string) => {
        try {
            await axios.delete(`${SERVER_URL}/api/users/${userId}/contacts/${contactId}`);
            setContacts(prev => prev.filter(c => c.userId !== contactId));
            if (currentRecipientId === contactId) {
                onSelectContact('');
            }
        } catch (error) {
            onError('Failed to remove contact');
        }
        handleCloseMenu();
    };

    const handleBlockUser = async (contactId: string) => {
        try {
            await axios.post(`${SERVER_URL}/api/users/${userId}/blocked`, {
                blockUserId: contactId
            });
            // Remove from contacts if present
            setContacts(prev => prev.filter(c => c.userId !== contactId));
            // Add to blocked list locally or refetch
            fetchBlockedUsers();

            if (currentRecipientId === contactId) {
                onSelectContact('');
            }
        } catch (error) {
            onError('Failed to block user');
        }
        handleCloseMenu();
    };

    const handleUnblockUser = async (blockUserId: string) => {
        try {
            await axios.delete(`${SERVER_URL}/api/users/${userId}/blocked/${blockUserId}`);
            setBlockedUsers(prev => prev.filter(u => u.userId !== blockUserId));
        } catch (error) {
            onError('Failed to unblock user');
        }
    };

    const handleMenuClick = (event: React.MouseEvent<HTMLElement>, contact: Contact) => {
        setAnchorEl(event.currentTarget);
        setMenuContact(contact);
        event.stopPropagation();
    };

    const handleCloseMenu = () => {
        setAnchorEl(null);
        setMenuContact(null);
    };

    const getAvatarColor = (id: string) => {
        const colors = ['#2563eb', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4'];
        const index = id.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length;
        return colors[index];
    };

    return (
        <Paper
            elevation={2}
            sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                borderRadius: 3,
                border: '1px solid',
                borderColor: 'divider',
            }}
        >
            <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
                <Typography variant="h6" fontWeight="bold">Contacts</Typography>
            </Box>

            <Tabs
                value={tabValue}
                onChange={(_, v) => setTabValue(v)}
                variant="fullWidth"
                textColor="primary"
                indicatorColor="primary"
                sx={{ borderBottom: '1px solid', borderColor: 'divider' }}
            >
                <Tab label="My Contacts" />
                <Tab label="Blocked" />
            </Tabs>

            <Box sx={{ flexGrow: 1, overflowY: 'auto' }}>
                {tabValue === 0 ? (
                    <List disablePadding>
                        <ListItem disablePadding>
                            <ListItemButton onClick={() => setAddContactOpen(true)} sx={{ py: 2 }}>
                                <ListItemAvatar>
                                    <Avatar sx={{ bgcolor: 'secondary.main' }}>
                                        <PersonAddIcon />
                                    </Avatar>
                                </ListItemAvatar>
                                <ListItemText
                                    primary="Add New Contact"
                                    secondary="By User ID or Phone Number"
                                />
                            </ListItemButton>
                        </ListItem>

                        {contacts.map((contact) => (
                            <ListItem
                                key={contact.userId}
                                disablePadding
                                secondaryAction={
                                    <IconButton
                                        size="small"
                                        onClick={(e) => handleMenuClick(e, contact)}
                                    >
                                        <MoreVertIcon fontSize="small" />
                                    </IconButton>
                                }
                            >
                                <ListItemButton
                                    selected={currentRecipientId === contact.userId}
                                    onClick={() => onSelectContact(contact.userId)}
                                    sx={{
                                        borderLeft: currentRecipientId === contact.userId ? '4px solid' : '4px solid transparent',
                                        borderColor: 'primary.main',
                                        bgcolor: currentRecipientId === contact.userId ? 'action.selected' : 'inherit'
                                    }}
                                >
                                    <ListItemAvatar>
                                        <Avatar
                                            src={contact.profilePicture}
                                            sx={{ bgcolor: getAvatarColor(contact.userId) }}
                                        >
                                            {contact.displayName ? contact.displayName[0].toUpperCase() : <PersonIcon />}
                                        </Avatar>
                                    </ListItemAvatar>
                                    <ListItemText
                                        primary={contact.displayName || contact.userId}
                                        secondary={
                                            <Typography variant="caption" noWrap display="block" color="text.secondary">
                                                {contact.mood || contact.phoneNumber || contact.userId}
                                            </Typography>
                                        }
                                    />
                                </ListItemButton>
                            </ListItem>
                        ))}

                        {contacts.length === 0 && (
                            <Box sx={{ p: 3, textAlign: 'center', opacity: 0.6 }}>
                                <Typography variant="body2">No contacts yet.</Typography>
                            </Box>
                        )}
                    </List>
                ) : (
                    <List>
                        {blockedUsers.map((user) => (
                            <ListItem key={user.userId}>
                                <ListItemAvatar>
                                    <Avatar src={user.profilePicture}>
                                        <BlockIcon />
                                    </Avatar>
                                </ListItemAvatar>
                                <ListItemText
                                    primary={user.displayName || user.userId}
                                    secondary="Blocked"
                                />
                                <ListItemSecondaryAction>
                                    <Button
                                        size="small"
                                        color="error"
                                        onClick={() => handleUnblockUser(user.userId)}
                                    >
                                        Unblock
                                    </Button>
                                </ListItemSecondaryAction>
                            </ListItem>
                        ))}
                        {blockedUsers.length === 0 && !loading && (
                            <Box sx={{ p: 3, textAlign: 'center', opacity: 0.6 }}>
                                <Typography variant="body2">No blocked users.</Typography>
                            </Box>
                        )}
                    </List>
                )}
            </Box>

            {/* Context Menu */}
            <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={handleCloseMenu}
                disablePortal
            >
                <MenuItem onClick={() => menuContact && handleRemoveContact(menuContact.userId)}>
                    <ListItemIcon><DeleteIcon fontSize="small" /></ListItemIcon>
                    <ListItemText>Remove Contact</ListItemText>
                </MenuItem>
                <MenuItem onClick={() => menuContact && handleBlockUser(menuContact.userId)}>
                    <ListItemIcon><BlockIcon fontSize="small" /></ListItemIcon>
                    <ListItemText>Block User</ListItemText>
                </MenuItem>
            </Menu>

            {/* Add Contact Dialog */}
            <Dialog open={addContactOpen} onClose={() => setAddContactOpen(false)} maxWidth="xs" fullWidth>
                <DialogTitle>Add Contact</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="User ID or Phone Number"
                        fullWidth
                        variant="outlined"
                        value={newContactId}
                        onChange={(e) => setNewContactId(e.target.value)}
                        placeholder="+1234567890 or user_..."
                        helperText="Enter the phone number (with country code) or user ID of the person you want to add."
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setAddContactOpen(false)}>Cancel</Button>
                    <Button
                        onClick={handleAddContact}
                        variant="contained"
                        disabled={addingContact || !newContactId.trim()}
                    >
                        {addingContact ? <CircularProgress size={24} /> : 'Add'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Paper>
    );
}
