package com.mail.springbootimaplistener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import static com.fasterxml.jackson.databind.type.LogicalType.DateTime;
import com.mail.springbootimaplistener.entity.DocumentTypes;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.mail.springbootimaplistener.entity.IncomingEmails;
import com.mail.springbootimaplistener.entity.IncomingEmailAttachments;
import com.mail.springbootimaplistener.repository.DocumentTypeRepository;
import com.mail.springbootimaplistener.repository.IncomingEmailAttachmentRepository;
import com.mail.springbootimaplistener.repository.IncomingEmailRepository;
import com.mail.springbootimaplistener.repository.VendorRepository;
import com.mail.springbootimaplistener.response.ResponseHandler;

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
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;
import org.wildfly.common.Assert;

@Service
public class ReceiveMailServiceImpl implements ReceiveMailService {

    private static final Logger log = LoggerFactory.getLogger(ReceiveMailServiceImpl.class);

    private static final String DOWNLOAD_FOLDER = "Request";

    private static final String DOWNLOADED_MAIL_FOLDER = "DOWNLOADED";

    @Autowired
    private IncomingEmailRepository incomingEmailRepository;

    @Autowired
    private IncomingEmailAttachmentRepository incomingEmailAttachmentRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private DocumentTypeRepository documentTypeRepository;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private RestTemplate restTemplate;

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

        Timestamp ldt = null;

        String subject = mimeMessageParser.getSubject();
        Boolean documentRequest = subject.contains("Document Request");
        System.out.println("Apakah ada kalimat Document Request? : " + documentRequest);
        String vendorId = subject.split(":")[1].trim();
        int count = 0;
        for (int i = 0; i < vendorId.length(); i++) {
            if (vendorId.charAt(i) != ' ') {
                count++;
            }
        }
        //Jika subject mengandung pesan "Document Request" dan id:<string dengan panjang 36 karakter> maka email ini diidentifikasi berasal dari vendor
        if (documentRequest == true && count == 36) {
            String vendor = vendorRepository.findVendorId(vendorId);
            System.out.println("Vendor Id : " + vendor);
            if (!vendor.isEmpty()) { //Jika vendor id ditemukan
                String sender = mimeMessageParser.getFrom();
                List<Address> recipients = mimeMessageParser.getTo();
                String recipients2 = recipients.toString().substring(1, recipients.toString().length() - 1);
                List<Address> cc = mimeMessageParser.getCc();
                String cc2 = cc.toString().substring(1, cc.toString().length() - 1);

                String body = mimeMessageParser.getPlainContent();

                String receivedTime = mimeMessageParser.getMimeMessage().getReceivedDate().toString();

                DateTimeFormatter f = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu", Locale.US);
                ZonedDateTime zdt = ZonedDateTime.parse(receivedTime, f);

                String localdatetime = zdt.toLocalDateTime().toString();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                LocalDateTime localDate = LocalDateTime.parse(localdatetime, formatter);

                String t = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(localDate);
                ldt = Timestamp.valueOf(t);

                incomingEmailRepository.insertIncomingEmail(sender, recipients2, cc2, subject, body, ldt);

                Map<String, Object> requestAPI = new HashMap<>();
                //foreach attachment
                mimeMessageParser.getAttachmentList().forEach(dataSource -> {
                    if (StringUtils.isNotBlank(dataSource.getName())) {
                        String data = dataSource.getName();
                        List<String> documentType = documentTypeRepository.findKeywords();
                        String rename_file = null;
                        String validasi = null;
                        String transformKey = null;
                        String transformKey2 = null;
                        String extension = null;
                        int documentTypeId = 0;
                        String expired_date = null;
                        String tempDate = null;
                        String replace2 = null;
                        String replace3 = null;
                        int number = 0;
                        String number2 = null;
                        String vendorIdVendDoc = null;
                        int countDigit = 0;
                        Timestamp date = new Timestamp(System.currentTimeMillis());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String date2 = sdf.format(date);

                        for (String docType : documentType) {
                            String keywords2 = null;
                            int pass = 0;
                            documentTypeId = documentTypeRepository.getId(docType);
                            System.out.println("docType : " + docType);

                            keywords2 = data.substring(0, data.indexOf("."));
                            extension = data.substring(data.lastIndexOf("."));
                            System.out.println("Keywords2 : " + keywords2);
                            System.out.println("extension : " + extension);
                            transformKey = keywords2.toUpperCase();
                            System.out.println("Uppercase keywords2 : " + transformKey);

                            pass = 0;
                            System.out.println("Apakah ada koma pada keyword DB? : " + docType.contains(","));
                            if (pass == 0) {
                                if (docType.contains(",") == true) {
                                    //Boolean keyNIB = docType.contains(ketemu);
                                    //System.out.println("Apakah ada keyword NIB pada keyword DB ? : " + keyNIB);
                                    System.out.println("Doctype ada koma : " + docType);
                                    String[] doc = docType.split("\\s*,\\s*");
                                    List<String> items = Arrays.asList(doc);
                                    for (String res : items) {
                                        System.out.println("Res : " + res);
                                        if (transformKey.contains(res) == true) {
                                            System.out.println("Filename yang masuk : " + transformKey);
                                            pass = 1;
                                            if (pass == 1) {
                                                if (documentTypeRepository.findFileNameFormat(res).contains("YYYYMMDD") == true) {
                                                    transformKey2 = transformKey.replaceAll("[^0-9]+", " ");
                                                    System.out.println("Transform Key : " + transformKey2);
                                                    if (transformKey2 != null) {
                                                        List<String> replace = Arrays.asList(transformKey2.trim().split(" "));
                                                        replace2 = replace.toString().substring(1, replace.toString().length() - 1);
                                                        System.out.println("Replace2 : " + replace2);
                                                        if (replace2 != null) {
                                                            //for (String replace2 : replace) {
                                                            //for (String subString : replace2.trim().split(",")) {
                                                            /*for (int i = 0, len = replace2.length(); i < len; i++) {
                                                                if (Character.isDigit(replace2.charAt(i))) {
                                                                    countDigit++;
                                                                    break;
                                                                }
                                                            }*/
                                                            //}
                                                            String wordsWithQuotes[] = replace2.split(",");
                                                            number = wordsWithQuotes.length;
                                                            number2 = String.valueOf(number);
                                                            //System.out.println("no of words = " + wordsWithQuotes.length);
                                                            System.out.println("Count Digit : " + number2);
                                                            for (String s : wordsWithQuotes) {
                                                                String[] s2 = s.trim().split(" ");
                                                                for (String results : s2) {
                                                                    if (results.length() == 8) {
                                                                        System.out.println("Results yang mengandung 8 digit : " + results);

                                                                        try {
                                                                            System.out.println("Transform Key 2 : " + transformKey);
                                                                            tempDate = results;
                                                                            System.out.println("TempDate : " + results);
                                                                            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
                                                                            LocalDate ld = LocalDate.parse(results, format);

                                                                            String ld2 = String.valueOf(ld);
                                                                            validasi = ld2.replace("-", "");
                                                                            System.out.println("LocalDate : " + ld2);
                                                                            System.out.println("LocalDate Remove (-) : " + validasi);

                                                                        } catch (DateTimeParseException excep) {
                                                                            System.out.println("Error date format : " + excep);
                                                                        }

                                                                        if (validasi != null) {
                                                                            expired_date = validasi;
                                                                            rename_file = res + "_" + expired_date + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                        } else {
                                                                            expired_date = "99991231";
                                                                            rename_file = res + "_" + expired_date + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                        }

                                                                    } else {
                                                                        rename_file = res + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                    }
                                                                    //break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    //break;
                                                } else {
                                                    rename_file = res + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                }
                                            }
                                            //break;
                                        }
                                    }
                                    //break;
                                } else {
                                    System.out.println("DocType tanpa comma : " + docType);
                                    List<String> items2 = Arrays.asList(docType);

                                    for (String res : items2) {
                                        System.out.println("Res : " + res);
                                        if (transformKey.contains(res) == true) {
                                            System.out.println("Filename yang masuk : " + transformKey);
                                            pass = 1;
                                            if (pass == 1) {
                                                if (documentTypeRepository.findFileNameFormat(res).contains("YYYYMMDD") == true) {
                                                    transformKey2 = transformKey.replaceAll("[^0-9]+", " ");
                                                    System.out.println("Transform Key : " + transformKey2);
                                                    if (transformKey2 != null) {
                                                        List<String> replace = Arrays.asList(transformKey2.trim().split(" "));
                                                        replace2 = replace.toString().substring(1, replace.toString().length() - 1);
                                                        System.out.println("Replace2 : " + replace2);
                                                        if (replace2 != null) {
                                                            //for (String replace2 : replace) {
                                                            //for (String subString : replace2.trim().split(",")) {
                                                            /*for (int i = 0, len = replace2.length(); i < len; i++) {
                                                                if (Character.isDigit(replace2.charAt(i))) {
                                                                    countDigit++;
                                                                    break;
                                                                }
                                                            }*/
                                                            //}
                                                            String wordsWithQuotes[] = replace2.split(",");
                                                            number = wordsWithQuotes.length;
                                                            number2 = String.valueOf(number);
                                                            //System.out.println("no of words = " + wordsWithQuotes.length);
                                                            System.out.println("Count Digit : " + number2);
                                                            for (String s : wordsWithQuotes) {
                                                                String[] s2 = s.trim().split(" ");
                                                                for (String results : s2) {
                                                                    if (results.length() == 8) {
                                                                        System.out.println("Results yang mengandung 8 digit : " + results);

                                                                        try {
                                                                            System.out.println("Transform Key 2 : " + transformKey);
                                                                            tempDate = results;
                                                                            System.out.println("TempDate : " + results);
                                                                            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
                                                                            LocalDate ld = LocalDate.parse(results, format);

                                                                            String ld2 = String.valueOf(ld);
                                                                            validasi = ld2.replace("-", "");
                                                                            System.out.println("LocalDate : " + ld2);
                                                                            System.out.println("LocalDate Remove (-) : " + validasi);

                                                                        } catch (DateTimeParseException excep) {
                                                                            System.out.println("Error date format : " + excep);
                                                                        }

                                                                        if (validasi != null) {
                                                                            expired_date = validasi;
                                                                            rename_file = res + "_" + expired_date + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                        } else {
                                                                            expired_date = "99991231";
                                                                            rename_file = res + "_" + expired_date + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                        }

                                                                    } else {
                                                                        rename_file = res + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                    }
                                                                    //break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    //break;
                                                } else {
                                                    rename_file = res + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                }
                                            }
                                        } /*else if(transformKey.contains(res) == false){
                                            System.out.println("File does not contain any keyword");
                                            SimpleMailMessage msg = new SimpleMailMessage();

                                            msg.setFrom(recipients2);
                                            msg.setTo(sender);

                                            msg.setSubject("Document's file name didn't matched");
                                            msg.setText("Hello. Please send the document according rules and if there is an expired date, please provide it with a valid date format {YYYYMMDD}. Thank you :)");

                                            javaMailSender.send(msg);
                                        }*/ else {
                                            System.out.println("Tidak ada");
                                        }

                                    }

                                }
                            }
                        }

                        System.out.println(data);

                        String folder = null;
                        String request = null;
                        String request2 = null;
                        try {
                            folder = mimeMessageParser.getSubject();
                            request = folder.split(":")[1];
                            request2 = request.trim();
                        } catch (Exception ex) {
                            java.util.logging.Logger.getLogger(ReceiveMailServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                        //String rootDirectoryPath = "/opt/wildfly-26.0.1.Final/welcome-content/attachments";
                        String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + request2;
                        createDirectoryIfNotExists(dataFolderPath);

                        if (rename_file != null) {

                            List<String> addFile = new ArrayList<String>(Arrays.asList(rename_file));
                            String addFile2 = addFile.toString().substring(1, addFile.toString().length() - 1);

                            //String filename = dataSource.getName();
                            //String filename2 = addFile2.substring(0, addFile2.lastIndexOf(".")) + "_" + date2 + addFile2.substring(addFile2.lastIndexOf("."));
                            String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + request2 + File.separator + addFile2;
                            File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);

                            log.info("Save attachment file to: {}", downloadedAttachmentFilePath);

                            Timestamp sentTime2 = new Timestamp(System.currentTimeMillis());
                            String now = String.valueOf(sentTime2);
                            System.out.println("Now : " + now);

                            requestAPI.put("vendorId", vendor);
                            requestAPI.put("documentTypeId", documentTypeId);
                            requestAPI.put("fileName", addFile2);
                            requestAPI.put("expiryDate", expired_date);
                            requestAPI.put("receivedTime", now);

                            ObjectMapper mapper = new ObjectMapper();
                            String clientFilterJson = "";
                            try {
                                clientFilterJson = mapper.writeValueAsString(requestAPI);
                                System.out.println("Client filter json : " + clientFilterJson);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                String receivedTime2 = mimeMessageParser.getMimeMessage().getReceivedDate().toString();
                                //Locale locale = new Locale("id", "ID");
                                DateTimeFormatter f2 = DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss z uuuu", Locale.US);
                                ZonedDateTime zdt2 = ZonedDateTime.parse(receivedTime2, f2);

                                //System.out.println(zdt);
                                String localdatetime2 = zdt2.toLocalDateTime().toString();
                                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                                LocalDateTime localDate2 = LocalDateTime.parse(localdatetime2, formatter2);

                                String t2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(localDate2);
                                Timestamp ldt2 = Timestamp.valueOf(t2);

                                incomingEmailAttachmentRepository.insertIncomingEmailAttachment(ldt2, addFile2);
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

                    }
                });
                /*// create headers
                HttpHeaders headers = new HttpHeaders();
                // set `content-type` header
                headers.setContentType(MediaType.APPLICATION_JSON);
                // set `accept` header
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                // build the request
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestAPI, headers);

                // send POST request
                ResponseEntity<String> response = restTemplate.postForEntity("http://156.67.219.250:7070/api/vendor/document/receive", entity, String.class);
                
                System.out.println("Response : " + response);//ResponseHandler.generateResponse(true, HttpStatus.OK, null, response);
                 */
            }

        }

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

    private static List<String> deleteEmptyStrings(List<String> strings) {
        List<String> filteredList = new ArrayList<String>();

        for (String string : strings) {

            if (string != null) {
                filteredList.add(string);
            }
        }
        return filteredList;
    }

}
