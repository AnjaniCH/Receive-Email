package com.mail.springbootimaplistener.service;

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
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
            String find = vendorRepository.findVendorId(vendorId);
            System.out.println("Vendor Id : " + find);
            if (!find.isEmpty()) { //Jika vendor id ditemukan
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
                //foreach attachment
                mimeMessageParser.getAttachmentList().forEach(dataSource -> {
                    if (StringUtils.isNotBlank(dataSource.getName())) {
                        String data = dataSource.getName();
                        List<String> documentType = documentTypeRepository.findKeywords();
                        String getDate = null;
                        String getNib = null;
                        String validasi = null;
                        String transformKey = null;
                        String transformKey2 = null;
                        String outKey = null;
                        String ketemu = null;
                        String extension = null;
                        String digit = null;
                        Boolean checkKeywords = true;
                        int countDigit = 0;
                        Timestamp date = new Timestamp(System.currentTimeMillis());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String date2 = sdf.format(date);

                        for (String docType : documentType) {
                            String keywords2 = null;
                            int pass = 0;
                            int docTypeId = 0;
                            System.out.println("docType : " + docType);

                            keywords2 = data.substring(0, data.indexOf("."));
                            extension = data.substring(data.lastIndexOf("."));
                            System.out.println("Keywords2 : " + keywords2);
                            System.out.println("extension : " + extension);
                            transformKey = keywords2.toUpperCase();
                            System.out.println("Uppercase keywords2 : " + transformKey);
                            /*if (transformKey.contains("NPWP")) {
                                Pattern p = Pattern.compile("NPWP");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            } else if (docType.contains("NIB")) {
                                Pattern p = Pattern.compile("NIB");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            } else if (transformKey.contains("TDP")) {
                                Pattern p = Pattern.compile("TDP");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            } else if (transformKey.contains("SIUP")) {
                                Pattern p = Pattern.compile("SIUP");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            } else if (transformKey.contains("BAI")) {
                                Pattern p = Pattern.compile("BAI");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            } else if (transformKey.contains("KTP")) {
                                Pattern p = Pattern.compile("KTP");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            } else if (transformKey.contains("ID Card")) {
                                Pattern p = Pattern.compile("ID Card");   // the pattern to search for
                                Matcher m = p.matcher(docType);
                                while (m.find()) {
                                    ketemu = m.group();
                                    System.out.println("Ketemu : " + ketemu);
                                }
                            }*/

                            //System.out.println("Keywords DB : " + ketemu);
                            pass = 0;
                            System.out.println("Apakah ada koma pada keyword DB? : " + docType.contains(","));
                            if (pass == 0) {
                                if (docType.contains(",") == true) {
                                    //Boolean keyNIB = docType.contains(ketemu);
                                    //System.out.println("Apakah ada keyword NIB pada keyword DB ? : " + keyNIB);
                                    if (docType.contains("NIB") == true) {
                                        String[] doc = docType.split("\\s*,\\s*");
                                        List<String> items = Arrays.asList(doc);
                                        //List<String> items = Arrays.asList(ketemu.split("\\s*,\\s*"));
                                        //System.out.println("NIB,TDP sebelum displit : " + items);
                                        for (String result : items) {
                                            System.out.println("Result : " + result);
                                            Boolean trans = transformKey.contains(result);
                                            System.out.println("Filename contains result : " + trans);
                                            if (transformKey.contains(result) == true) {
                                                pass = 1;
                                                //docTypeId = documentTypeRepository.getId(keywords2);
                                                //System.out.println("DocumentTypeID : " + result);
                                                if (result.equals("NIB")) {
                                                    StringBuilder sb = new StringBuilder();
                                                    boolean found = false;    
                                                    transformKey2 = transformKey.replaceAll("[^0-9]+", " ");
                                                    System.out.println("Transform Key : " + transformKey2);
                                                    List<String> replace = Arrays.asList(transformKey2.trim().split(" "));
                                                    /*for (char c : transformKey.toCharArray()) {
                                                        if (Character.isDigit(c)) {
                                                            sb.append(c);
                                                            found = true;
                                                        } else if (found) {
                                                            // If we already found a digit before and this char is not a digit, stop looping
                                                            break;
                                                        }
                                                    }*/
                                                    digit = replace.toString().substring(1, replace.toString().length() - 1);
                                                    System.out.println("Digit : " + digit);

                                                    if (digit != null) {
                                                        //String str = "plane,cat,red,dogy";
                                                        for (String subString : digit.split(",")) {
                                                            
                                                            for (int i = 0; i < subString.length(); i++) {
                                                                if (subString.charAt(i) != ' ') {
                                                                    countDigit++;
                                                                }
                                                            }
                                                        }
                                                        //String digit2[] = digit.split(" ");
                                                        /*List<String> al = Arrays.asList(digit);
                                                        String al2 = al.toString().substring(1, al.toString().length() - 1);
                                                        System.out.println("Al2 : " + al2);*/

                                                        System.out.println("Count Digit : " + countDigit);
                                                        if (countDigit == 8) {
                                                            try {
                                                                DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
                                                                //convert String to LocalDate
                                                                LocalDate ld = LocalDate.parse(digit, format);
                                                                
                                                                //REVISI
                                                                ////////////////////////////////////////////////////////////////
                                                                String ld2 = String.valueOf(ld);
                                                                String ld3 = ld2.replace("-", "");
                                                                System.out.println("LocalDate : " + ld2);
                                                                System.out.println("LocalDate Remove (-) : " + ld3);
                                                                
                                                                if(ld == null){
                                                                    
                                                                }
                                                                ///////////////////////////////////////////////////////////////
                                                                DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyyMMdd");
                                                                validasi = ld.format(format2);

                                                                System.out.println("Validasi : " + validasi);
                                                                Boolean valid = transformKey.contains(validasi);
                                                                System.out.println("Apakah file valid ? : " + valid);
                                                                if (transformKey.contains(result) == true && transformKey.contains(digit) == true && valid == true && digit.equals(validasi)) {
                                                                    System.out.println("Data terverifikasi : " + result);
                                                                    getNib = keywords2 + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                                    System.out.println("Data dengan expiration date ditambah date hari ini : " + getNib);
                                                                } else {
                                                                    System.out.println("not matched");
                                                                    SimpleMailMessage msg = new SimpleMailMessage();

                                                                    msg.setFrom(recipients2);
                                                                    msg.setTo(sender);

                                                                    msg.setSubject("Document contain wrong date format");
                                                                    msg.setText("Hello. Please send the document with valid date format {YYYYMMDD}");

                                                                    javaMailSender.send(msg);
                                                                }
                                                            } catch (DateTimeParseException excep) {
                                                                System.out.println("Error date format : " + excep);
                                                                //throw new IllegalArgumentException("Not able to parse the date for all patterns given");
                                                                System.out.println("not matched");
                                                                SimpleMailMessage msg = new SimpleMailMessage();

                                                                msg.setFrom(recipients2);
                                                                msg.setTo(sender);

                                                                msg.setSubject("Document contain wrong date format");
                                                                msg.setText("Hello. Please send the document with valid date format {YYYYMMDD}");

                                                                javaMailSender.send(msg);
                                                            }

                                                        } else {
                                                            getNib = data.substring(0, data.lastIndexOf(".")) + "_" + "99991231" + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                        }
                                                        /*//validasi expired time
                                                        System.out.println("Result NIB : " + result);
                                                        getDate = data.substring(data.lastIndexOf("_") + 1, data.indexOf("."));
                                                        System.out.println("Expired Date : " + getDate);
                                                        int countDate = 0;
                                                        int sizeDate = 8;
                                                        for (int i = 0; i < getDate.length(); i++) {
                                                            if (getDate.charAt(i) != ' ') {
                                                                countDate++;
                                                            }
                                                        }
                                                        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
                                                        //convert String to LocalDate
                                                        LocalDate ld = LocalDate.parse(getDate, formatter);

                                                        DateTimeFormatter format2 = DateTimeFormatter.ofPattern("yyyyMMdd");
                                                        validasi = ld.format(format2);
                                                        //String validasi = validasiFormat.format(getDate);
                                                        System.out.println("Validasi : " + validasi);*/

                                                    } else {
                                                        getNib = data.substring(0, data.lastIndexOf(".")) + "_" + date2 + data.substring(data.lastIndexOf("."));
                                                    }
                                                } else if (result.equals("TDP")) {
                                                    System.out.println("Result TDP : " + result);
                                                }

                                            }
                                        }
                                    }
                                    /*else if (keywords2.contains("KTP,ID Card")) {
                                        List<String> items2 = Arrays.asList(keywords2.split("\\s*,\\s*"));
                                        for (String result : items2) {
                                            if (data.contains(result) == true) {
                                                pass = 1;
                                                docTypeId = documentTypeRepository.getId(keywords2);
                                            }
                                        }
                                    } else {

                                    }*/
                                }
                            }
                        }

                        System.out.println(data);

                        String newdata = null;
                        String getNpwp = null;

                        String getSiup = null;
                        String getBai = null;
                        String keywords = null;
                        String file_name = null;
                        String ext = null;

                        /*if (data.contains("_") == true) {
                            getDate = data.substring(data.lastIndexOf("_") + 1, data.indexOf("."));
                            System.out.println(getDate);
                            int count2 = 0;
                            int size = 8;
                            for (int i = 0; i < getDate.length(); i++) {
                                if (getDate.charAt(i) != ' ') {
                                    count2++;
                                }
                            }
                            keywords = data.substring(0, data.indexOf("_"));
                            //String gabung = keywords + "_" + getDate + ".pdf";
                            file_name = documentTypeRepository.findFileNameFormat(keywords);
                            if (keywords.equals("NIB") && file_name.contains("NIB_${YYYYMMDD}") == true && count2 == 8) {
                                System.out.println(file_name);
                                String getNib2 = file_name.replaceAll("(?i)\\s*(?:\\$\\{YYYYMMDD\\}?)", getDate);
                                System.out.println(getNib2);
                                getNib = getNib2.substring(0, getNib2.lastIndexOf(".")) + "_" + date2 + getNib2.substring(getNib2.lastIndexOf("."));
                                System.out.println(getNib);
                            } else if (keywords.equals("SIUP") && file_name.contains("SIUP_${YYYYMMDD}") == true && count2 == 8) {
                                System.out.println(file_name);
                                String getSiup2 = file_name.replaceAll("(?i)\\s*(?:\\$\\{YYYYMMDD\\}?)", getDate);
                                System.out.println(getSiup2);
                                getSiup = getSiup2.substring(0, getSiup2.lastIndexOf(".")) + "_" + date2 + getSiup2.substring(getSiup2.lastIndexOf("."));
                                System.out.println(getSiup);
                            } else if (keywords.isEmpty() || keywords == null && file_name.isEmpty() || file_name == null && count2 > size) {
                                System.out.println("not matched");
                                SimpleMailMessage msg = new SimpleMailMessage();

                                msg.setFrom(recipients2);
                                msg.setTo(sender);

                                msg.setSubject("Document cannot be identified");
                                msg.setText("Hello. Please send the document according to the email template, thank you");

                                javaMailSender.send(msg);
                            }
                        } else {
                            keywords = data.substring(0, data.indexOf("."));
                            file_name = documentTypeRepository.findFileNameFormat(keywords);
                            if (keywords.equals("NPWP") && file_name.contains("NPWP") == true) {
                                getNpwp = file_name.substring(0, file_name.lastIndexOf(".")) + "_" + date2 + file_name.substring(file_name.lastIndexOf("."));
                                System.out.println(getNpwp);
                            } else if (keywords.equals("BAI") && file_name.contains("BAI") == true) {
                                getBai = file_name.substring(0, file_name.lastIndexOf(".")) + "_" + date2 + file_name.substring(file_name.lastIndexOf("."));
                                System.out.println(getBai);
                            } else if (keywords.isEmpty() || keywords == null && file_name.isEmpty() || file_name == null) {
                                System.out.println("not matched");
                                SimpleMailMessage msg = new SimpleMailMessage();

                                msg.setFrom(recipients2);
                                msg.setTo(sender);

                                msg.setSubject("Document cannot be identified");
                                msg.setText("Hello. Please send the document according to the email template, thank you");

                                javaMailSender.send(msg);
                            }
                        }*/
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

                        List<String> addFile = new ArrayList<String>(Arrays.asList(getNib));
                        addFile.removeAll(Arrays.asList(" ", "", null));
                        //addFile.removeIf(o->o == null);
                        //List<String> listWithoutNulls = addFile.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());
                        //addFile.removeIf(Objects::isNull);
                        //List<String> hasil = addFile.stream().filter(Objects::nonNull).collect(Collectors.toList());
                        //strings.stream().filter(string -> !string.isEmpty()).collect(Collectors.toList());
                        //List<String> filtered = deleteEmptyStrings(addFile);
                        //filtered = filtered.stream().filter(d -> d != null).collect(Collectors.toList());

                        String addFile2 = addFile.toString().substring(1, addFile.toString().length() - 1);

                        //String filename = dataSource.getName();
                        //String filename2 = addFile2.substring(0, addFile2.lastIndexOf(".")) + "_" + date2 + addFile2.substring(addFile2.lastIndexOf("."));
                        String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + request2 + File.separator + addFile2;
                        File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);

                        log.info("Save attachment file to: {}", downloadedAttachmentFilePath);
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
                });
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
