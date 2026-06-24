package com.frezo.email.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.email.dto.response.EmailInboxResponse;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.repository.EmailConfigRepository;
import com.frezo.email.service.EmailInboxService;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailInboxServiceImpl implements EmailInboxService {

    private final EmailConfigRepository emailConfigRepository;

    @Override
    public List<EmailInboxResponse> fetchInbox(String configId, String folder, int page, int size) {
        EmailConfig config = getConfig(configId);
        Store store = null;
        Folder imapFolder = null;

        try {
            Session session = createImapSession();
            store = session.getStore("imaps");
            String imapHost = deriveImapHost(config.getSmtp());
            store.connect(imapHost, 993, config.getNameEmail(), config.getApiKey());

            String folderName = resolveFolderName(store, folder);
            imapFolder = store.getFolder(folderName);
            imapFolder.open(Folder.READ_ONLY);

            int totalMessages = imapFolder.getMessageCount();
            if (totalMessages == 0) return Collections.emptyList();

            int endMsg = totalMessages - page * size;
            int startMsg = Math.max(1, endMsg - size + 1);
            if (endMsg < 1) return Collections.emptyList();
            Message[] messages = imapFolder.getMessages(startMsg, endMsg);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            fp.add(UIDFolder.FetchProfileItem.UID);
            imapFolder.fetch(messages, fp);

            List<EmailInboxResponse> result = new ArrayList<>();
            for (int i = messages.length - 1; i >= 0 && result.size() < size; i--) {
                result.add(toResponseLight(messages[i]));
            }

            return result;
        } catch (MessagingException e) {
            log.error("IMAP error fetchInbox config={} folder={}: {}", configId, folder, e.getMessage(), e);
            throw new QTHTException("error.email.inbox.fetch.failed: " + e.getMessage());
        } finally {
            closeQuietly(imapFolder, store);
        }
    }

    private String resolveFolderName(Store store, String folder) throws MessagingException {
        if (folder == null || folder.isBlank() || "inbox".equalsIgnoreCase(folder)) {
            return "INBOX";
        }

        // 1. Try folder as-is
        if (store.getFolder(folder).exists()) {
            return folder;
        }

        // 2. Extract simple name (last part after any /) e.g. "[Gmail]/Spam" -> "Spam"
        String simpleName = folder.contains("/") ? folder.substring(folder.lastIndexOf("/") + 1) : folder;

        // 3. Build candidate names for common locales
        List<String> candidates = new ArrayList<>();
        switch (simpleName.toLowerCase()) {
            case "sent" -> candidates.addAll(Arrays.asList("Sent Mail", "Sent", "Đã gửi", "Thư đã gửi"));
            case "drafts", "draft" -> candidates.addAll(Arrays.asList("Drafts", "Draft", "Nháp", "Bản nháp"));
            case "spam" -> candidates.addAll(Arrays.asList("Spam", "Thư rác", "Rác"));
            case "trash", "deleted", "bin" -> candidates.addAll(Arrays.asList("Trash", "Thùng rác", "Deleted Items", "Bin", "Rác"));
            case "starred" -> candidates.addAll(Arrays.asList("Starred", "Star", "Đã gắn sao", "Gắn sao"));
            case "all" -> candidates.addAll(Arrays.asList("All Mail", "All", "Tất cả thư"));
            case "important" -> candidates.addAll(Arrays.asList("Important", "Quan trọng"));
            default -> candidates.add(simpleName);
        }

        // 4. Try each candidate directly (with and without [Gmail]/ prefix)
        for (String candidate : candidates) {
            for (String nameToTry : Arrays.asList(candidate, "[Gmail]/" + candidate)) {
                if (store.getFolder(nameToTry).exists()) {
                    return nameToTry;
                }
            }
        }

        // 5. Last resort: search all folders by keyword
        Folder[] allFolders = store.getDefaultFolder().list("*");
        for (Folder f : allFolders) {
            String fullName = f.getFullName();
            for (String candidate : candidates) {
                if (fullName.toLowerCase().contains(candidate.toLowerCase())) {
                    return fullName;
                }
            }
        }

        return "INBOX";
    }

    @Override
    public EmailInboxResponse fetchEmailById(String configId, long uid) {
        EmailConfig config = getConfig(configId);
        Store store = null;
        Folder imapFolder = null;

        try {
            Session session = createImapSession();
            store = session.getStore("imaps");
            String imapHost = deriveImapHost(config.getSmtp());
            store.connect(imapHost, 993, config.getNameEmail(), config.getApiKey());

            for (String folderName : new String[]{"INBOX", "[Gmail]/Sent Mail", "[Gmail]/Drafts", "[Gmail]/Trash"}) {
                try {
                    imapFolder = store.getFolder(folderName);
                    imapFolder.open(Folder.READ_ONLY);
                    Message msg = imapFolder.getMessage((int) uid);
                    if (msg != null) return toResponse(msg);
                } catch (MessagingException ignored) {
                }
            }
            throw new QTHTException("error.email.not.found");
        } catch (MessagingException e) {
            log.error("IMAP error fetchEmailById uid={}: {}", uid, e.getMessage(), e);
            throw new QTHTException("error.email.inbox.fetch.failed: " + e.getMessage());
        } finally {
            closeQuietly(imapFolder, store);
        }
    }

    @Override
    public void markAsRead(String configId, long uid) {
        EmailConfig config = getConfig(configId);
        Store store = null;
        Folder imapFolder = null;

        try {
            Session session = createImapSession();
            store = session.getStore("imaps");
            String imapHost = deriveImapHost(config.getSmtp());
            store.connect(imapHost, 993, config.getNameEmail(), config.getApiKey());

            imapFolder = store.getFolder("INBOX");
            imapFolder.open(Folder.READ_WRITE);

            Message msg = imapFolder.getMessage((int) uid);
            if (msg != null) {
                msg.setFlag(Flags.Flag.SEEN, true);
                log.info("Marked email {} as read", uid);
            }
        } catch (MessagingException e) {
            log.error("IMAP error markAsRead uid={}: {}", uid, e.getMessage(), e);
            throw new QTHTException("error.email.inbox.mark.read.failed: " + e.getMessage());
        } finally {
            closeQuietly(imapFolder, store);
        }
    }

    private Session createImapSession() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "10000");
        props.put("mail.imaps.timeout", "20000");
        props.put("mail.imaps.writetimeout", "10000");
        props.put("mail.imaps.ssl.trust", "*");
        return Session.getInstance(props);
    }

    private EmailInboxResponse toResponseLight(Message msg) throws MessagingException {
        EmailInboxResponse.EmailInboxResponseBuilder builder = EmailInboxResponse.builder();

        builder.messageId(String.valueOf(msg.getMessageNumber()));
        builder.subject(msg.getSubject());

        if (msg.getFrom() != null && msg.getFrom().length > 0) {
            if (msg.getFrom()[0] instanceof InternetAddress addr) {
                builder.from(addr.getAddress());
                builder.fromPersonal(addr.getPersonal());
            } else {
                builder.from(msg.getFrom()[0].toString());
            }
        }

        if (msg.getRecipients(Message.RecipientType.TO) != null) {
            builder.to(Arrays.stream(msg.getRecipients(Message.RecipientType.TO))
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        }

        if (msg.getSentDate() != null) {
            builder.sentDate(LocalDateTime.ofInstant(
                    msg.getSentDate().toInstant(), ZoneId.systemDefault()));
        }

        builder.seen(msg.getFlags().contains(Flags.Flag.SEEN));
        builder.hasAttachments(false);
        builder.bodyPreview("");

        return builder.build();
    }

    private EmailInboxResponse toResponse(Message msg) throws MessagingException {
        EmailInboxResponse.EmailInboxResponseBuilder builder = EmailInboxResponse.builder();

        builder.messageId(String.valueOf(msg.getMessageNumber()));
        builder.subject(msg.getSubject());

        if (msg.getFrom() != null && msg.getFrom().length > 0) {
            if (msg.getFrom()[0] instanceof InternetAddress addr) {
                builder.from(addr.getAddress());
                builder.fromPersonal(addr.getPersonal());
            } else {
                builder.from(msg.getFrom()[0].toString());
            }
        }

        if (msg.getRecipients(Message.RecipientType.TO) != null) {
            builder.to(Arrays.stream(msg.getRecipients(Message.RecipientType.TO))
                    .map(Object::toString)
                    .collect(Collectors.toList()));
        }

        if (msg.getSentDate() != null) {
            builder.sentDate(LocalDateTime.ofInstant(
                    msg.getSentDate().toInstant(), ZoneId.systemDefault()));
        }

        builder.seen(msg.getFlags().contains(Flags.Flag.SEEN));
        builder.hasAttachments(false);

        try {
            Object content = msg.getContent();
            if (content instanceof String text) {
                builder.bodyPreview(text.length() > 200 ? text.substring(0, 200) : text);
                builder.bodyHtml(text);
            } else if (content instanceof Multipart multipart) {
                processMultipart(multipart, builder);
            }
        } catch (IOException | MessagingException e) {
            log.warn("Failed to read email content: {}", e.getMessage());
        }

        return builder.build();
    }

    private void processMultipart(Multipart multipart, EmailInboxResponse.EmailInboxResponseBuilder builder)
            throws MessagingException, IOException {
        StringBuilder textContent = new StringBuilder();
        List<String> attachments = new ArrayList<>();

        for (int i = 0; i < multipart.getCount(); i++) {
            Part part = multipart.getBodyPart(i);

            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                attachments.add(part.getFileName());
                continue;
            }

            Object content = part.getContent();
            if (content instanceof String s) {
                textContent.append(s);
            } else if (content instanceof Multipart mp) {
                processMultipart(mp, builder);
            }
        }

        String text = textContent.toString();
        builder.bodyPreview(text.length() > 200 ? text.substring(0, 200) : text);
        builder.bodyHtml(text);
        if (!attachments.isEmpty()) {
            builder.hasAttachments(true);
            builder.attachmentNames(attachments);
        }
    }

    private EmailConfig getConfig(String configId) {
        return emailConfigRepository.findById(configId)
                .orElseThrow(() -> new QTHTException("error.email.config.not.found"));
    }

    private String deriveImapHost(String smtpHost) {
        if (smtpHost == null) return "imap.gmail.com";
        String lower = smtpHost.toLowerCase();
        if (lower.contains("gmail")) return "imap.gmail.com";
        if (lower.contains("outlook") || lower.contains("office365") || lower.contains("hotmail")) return "outlook.office365.com";
        if (lower.contains("yahoo")) return "imap.mail.yahoo.com";
        if (lower.startsWith("smtp.")) return "imap." + lower.substring(5);
        return smtpHost.replace("smtp", "imap");
    }

    private void closeQuietly(Folder folder, Store store) {
        try {
            if (folder != null && folder.isOpen()) folder.close(false);
            if (store != null && store.isConnected()) store.close();
        } catch (MessagingException e) {
            log.warn("Error closing IMAP connection: {}", e.getMessage());
        }
    }
}
