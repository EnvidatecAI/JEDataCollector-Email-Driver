/**
 * Copyright (C) 2013 - 2015 Envidatec GmbH <info@envidatec.com>
 *
 * This file is part of JEAPI.
 *
 * JEAPI is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation in version 3.
 *
 * JEAPI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JEAPI. If not, see <http://www.gnu.org/licenses/>.
 *
 * JEAPI is part of the OpenJEVis project, further project information are
 * published at <http://www.OpenJEVis.org/>.
 */
package org.jevis.emaildatasource;

/**
 *
 * @author A
 */
import javax.mail.*;
import javax.mail.search.SearchTerm;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SubjectTerm;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;

public class EmailForTest {

    private Long _id;
    private String _name; //
    private String _userName; // email
    private String _password;
    private String _host;
    private String _folderName;
    private Integer _connectionTimeout;
    private Integer _readTimeout;
    private Boolean _enabled;
    private String _timezone;
    private Store _store;
    private Folder _folder;
    private Session _session;

    public void testMail() throws MessagingException {
        List<InputStream> answerList = new ArrayList<InputStream>();

        _userName = "artur.iablokov@envidatec.com";
        _password = "na733aya";
        _host = "imap.1und1.de";

        Properties props = new Properties();
        props.put("mail.debug", "true");
        props.put("mail.store.protocol", "imaps");
        _session = Session.getInstance(props);
        _store = _session.getStore();
        _store.connect(_host, _userName, _password);
        if (!_store.isConnected()) {
            System.out.println("Connected not possible");
        }
        _folderName = "INBOX"; // TODO
        _folder = _store.getFolder(_folderName);
        _folder.open(Folder.READ_ONLY);

        System.out.println("//////////Folder open!/////");

        InputStream answer = null;

        //channel parameter bekommen
        String sender = "support@jevis.de";
        String subject = "testinfo";
        Date lastReadout = new Date(1459658993827L);
        System.out.println("channel parameters: " + sender + " " + subject + " " + lastReadout);
        //richtige E-Mail(-s) finden
        SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, lastReadout);
        SearchTerm senderTerm = null;
        try {
            senderTerm = new FromTerm(new InternetAddress(sender));
        } catch (AddressException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        SearchTerm subjectTerm = new SubjectTerm(subject);
        SearchTerm andTerm = new AndTerm(newerThan, new AndTerm(senderTerm, subjectTerm));
        System.out.println(andTerm.toString());
        List<Message> messageList = null;
        try {
            messageList = Arrays.asList(_folder.search(andTerm));
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //
        _folder.close(false);
        System.out.println("Folder closed");
        _store.close();
        System.out.println("Store closed");

        /**
         * List<File> attachments = new ArrayList<File>(); for (Message message
         * : messageList) { try { Multipart multipart = (Multipart)
         * message.getContent(); for (int i = 0; i < multipart.getCount(); i++)
         * { BodyPart bodyPart = multipart.getBodyPart(i); if
         * (!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
         * !StringUtils.isNotBlank(bodyPart.getFileName())) { // !Checks if a
         * String is not empty (""), not null and not whitespace only. continue;
         * // dealing with attachments only } InputStream is =
         * bodyPart.getInputStream(); File file = new File("/tmp/" +
         * bodyPart.getFileName()); FileOutputStream fos = new
         * FileOutputStream(file); byte[] buf = new byte[4096]; int bytesRead;
         * while ((bytesRead = is.read(buf)) != -1) { fos.write(buf, 0,
         * bytesRead); } fos.close(); attachments.add(file);
                }*
         */

    /*for (int i = 0; i < answerList.size(); i++) {
                System.out.print(answerList.get(i).toString());
                }*/
}


//class
}
