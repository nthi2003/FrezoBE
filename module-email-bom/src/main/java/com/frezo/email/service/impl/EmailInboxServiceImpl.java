package com.frezo.email.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.email.dto.response.EmailInboxResponse;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.repository.EmailConfigRepository;
import com.frezo.email.service.EmailInboxService;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
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
    public List<EmailInboxResponse> fetchInbox(String configId, int page, int size) {
        EmailConfig config = getConfig(configId);
        Store store = null;
        Folder inbox = null;

        try {
            Session session = createImapSession();
            store = session.getStore("imaps");
            String imapHost = deriveImapHost(config.getSmtp());
            store.connect(imapHost, 993, config.getNameEmail(), config.getApiKey());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            if (messages.length == 0) {
                messages = inbox.getMessages();
            }

            List<EmailInboxResponse> result = new ArrayList<>();
            for (int i = messages.length - 1 - page * size; i >= 0 && result.size() < size; i--) {
                result.add(toResponse(messages[i]));
            }

            return result;
        } catch (MessagingException e) {
            log.error("Failed to fetch inbox for config {}: {}", configId, e.getMessage());
            throw new QTHTException("error.email.inbox.fetch.failed");
        } finally {
            closeQuietly(inbox, store);
        }
    }

    @Override
    public EmailInboxResponse fetchEmailById(String configId, long uid) {
        EmailConfig config = getConfig(configId);
        Store store = null;
        Folder inbox = null;

        try {
            Session session = createImapSession();
            store = session.getStore("imaps");
            String imapHost = deriveImapHost(config.getSmtp());
            store.connect(imapHost, 993, config.getNameEmail(), config.getApiKey());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            for (Message msg : messages) {
                if (msg instanceof MimeMessage && msg.getMessageNumber() == uid) {
                    return toResponse(msg);
                }
            }
            throw new QTHTException("error.email.not.found");
        } catch (MessagingException e) {
            log.error("Failed to fetch email {}: {}", uid, e.getMessage());
            throw new QTHTException("error.email.inbox.fetch.failed");
        } finally {
            closeQuietly(inbox, store);
        }
    }

    @Override
    public void markAsRead(String configId, long uid) {
        EmailConfig config = getConfig(configId);
        Store store = null;
        Folder inbox = null;

        try {
            Session session = createImapSession();
            store = session.getStore("imaps");
            String imapHost = deriveImapHost(config.getSmtp());
            store.connect(imapHost, 993, config.getNameEmail(), config.getApiKey());

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            for (Message msg : messages) {
                if (msg.getMessageNumber() == uid) {
                    msg.setFlag(Flags.Flag.SEEN, true);
                    log.info("Marked email {} as read", uid);
                    return;
                }
            }
        } catch (MessagingException e) {
            log.error("Failed to mark email {} as read: {}", uid, e.getMessage());
            throw new QTHTException("error.email.inbox.mark.read.failed");
        } finally {
            closeQuietly(inbox, store);
        }
    }

    private Session createImapSession() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "5000");
        props.put("mail.imaps.timeout", "10000");
        props.put("mail.imaps.ssl.trust", "*");
        return Session.getInstance(props);
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
        } catch (IOException e) {
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
