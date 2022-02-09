package com.mail.springbootimaplistener.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.mail.springbootimaplistener.entity.IncomingEmails;
import com.mail.springbootimaplistener.entity.IncomingEmailAttachments;
import com.mail.springbootimaplistener.repository.IncomingEmailRepository;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ReceiveMailServiceImpl implements ReceiveMailService {

    private static final Logger log = LoggerFactory.getLogger(ReceiveMailServiceImpl.class);

    private static final String DOWNLOAD_FOLDER = "data";

    private static final String DOWNLOADED_MAIL_FOLDER = "DOWNLOADED";

    @Autowired
    private IncomingEmailRepository incomingEmailRepository;

    @Override
    public void handleReceiveMail(MimeMessage receivedMessage) {
        try {

            Folder folder = receivedMessage.getFolder();
            folder.open(Folder.READ_WRITE);

            Message[] messages = folder.getMessages();
            fetchMessagesInFolder(folder, messages);

            Arrays.asList(messages).stream().filter(message -> {
                MimeMessage currentMessage = (MimeMessage) message;
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            }).forEach(this::extractMail);

            copyMailToDownloadedFolder(receivedMessage, folder);

            folder.close(true);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void fetchMessagesInFolder(Folder folder, Message[] messages) throws MessagingException {
        FetchProfile contentsProfile = new FetchProfile();
        contentsProfile.add(FetchProfile.Item.ENVELOPE);
        contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
        contentsProfile.add(FetchProfile.Item.FLAGS);
        contentsProfile.add(FetchProfile.Item.SIZE);
        folder.fetch(messages, contentsProfile);
    }

    private void copyMailToDownloadedFolder(MimeMessage mimeMessage, Folder folder) throws MessagingException {
        Store store = folder.getStore();
        Folder downloadedMailFolder = store.getFolder(DOWNLOADED_MAIL_FOLDER);
        if (downloadedMailFolder.exists()) {
            downloadedMailFolder.open(Folder.READ_WRITE);
            downloadedMailFolder.appendMessages(new MimeMessage[]{mimeMessage});
            downloadedMailFolder.close();
        }
    }

    private void extractMail(Message message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message;
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(messageToExtract).parse();

            showMailContent(mimeMessageParser);

            downloadAttachmentFiles(mimeMessageParser);

            // To delete downloaded email
            messageToExtract.setFlag(Flags.Flag.DELETED, true);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void showMailContent(MimeMessageParser mimeMessageParser) throws Exception {
        log.debug("Sender: {} | Recipients: {} | cc: {} | Subject: {} | ReceivedTime: {}", mimeMessageParser.getFrom(), mimeMessageParser.getTo(), mimeMessageParser.getCc(), mimeMessageParser.getSubject(), mimeMessageParser.getMimeMessage().getReceivedDate());
        log.debug("Body: {}", mimeMessageParser.getPlainContent());

    }

    private void downloadAttachmentFiles(MimeMessageParser mimeMessageParser) throws Exception {
        log.debug("Email has {} attachment files", mimeMessageParser.getAttachmentList().size());
        mimeMessageParser.getAttachmentList().forEach(dataSource -> {
            if (StringUtils.isNotBlank(dataSource.getName())) {
                String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER;
                createDirectoryIfNotExists(dataFolderPath);

                String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + dataSource.getName();
                File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);

                log.info("Save attachment file to: {}", downloadedAttachmentFilePath);

                try {
                    String sender = mimeMessageParser.getFrom();
                    List<Address> recipients = mimeMessageParser.getTo();
                    String recipients2 = recipients.toString().substring(1, recipients.toString().length() - 1);
                    List<Address> cc = mimeMessageParser.getCc();
                    String cc2 = cc.toString().substring(1, cc.toString().length() - 1);
                    String subject = mimeMessageParser.getSubject();
                    String body = mimeMessageParser.getPlainContent();

                    String receivedTime = mimeMessageParser.getMimeMessage().getReceivedDate().toString();
                    Locale locale = new Locale("id", "ID");
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu", Locale.US);
                    ZonedDateTime zdt = ZonedDateTime.parse(receivedTime, f);
                    
                    System.out.println(zdt);

                    String localdatetime = zdt.toLocalDateTime().toString();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                    LocalDateTime localDate = LocalDateTime.parse(localdatetime, formatter);

                    String t = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(localDate);
                    Timestamp ldt = Timestamp.valueOf(t);

                    String fileName = dataSource.getName();

                    IncomingEmails incomingEmail = new IncomingEmails(sender, recipients2, cc2, subject, body, ldt);
                    IncomingEmailAttachments incomingEmailAttachment = new IncomingEmailAttachments(fileName);
                    incomingEmail.getAttachments().add(incomingEmailAttachment);
                    incomingEmailRepository.save(incomingEmail);

                } catch (Exception e) {
                    log.error("Failed insert data", e);
                }
                try (
                        OutputStream out = new FileOutputStream(downloadedAttachmentFile) // InputStream in = dataSource.getInputStream()
                        ) {
                    InputStream in = dataSource.getInputStream();
                    IOUtils.copy(in, out);
                } catch (IOException e) {
                    log.error("Failed to save file.", e);
                }
            }
        });
    }

    private void createDirectoryIfNotExists(String directoryPath) {
        if (!Files.exists(Paths.get(directoryPath))) {
            try {
                Files.createDirectories(Paths.get(directoryPath));
            } catch (IOException e) {
                log.error("An error occurred during create folder: {}", directoryPath, e);
            }
        }
    }

}
