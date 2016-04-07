/**
 * Copyright (C) 2013 - 2016 Envidatec GmbH <info@envidatec.com>
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.*;
import org.jevis.api.JEVisObject;
import org.jevis.commons.driver.DataSource;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

import org.jevis.api.JEVisAttribute;
import org.jevis.api.JEVisClass;
import org.jevis.api.JEVisException;
import org.jevis.api.JEVisType;
import org.jevis.api.sql.JEVisDataSourceSQL;
import org.jevis.commons.DatabaseHelper;
import org.jevis.commons.cli.JEVisCommandLine;
import org.jevis.commons.driver.DataCollectorTypes;
import org.jevis.commons.driver.Importer;
import org.jevis.commons.driver.ImporterFactory;
import org.jevis.commons.driver.JEVisImporterAdapter;
import org.jevis.commons.driver.Parser;
import org.jevis.commons.driver.ParserFactory;
import org.jevis.commons.driver.Result;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import sun.misc.Launcher;


public class EMailDataSource implements DataSource{
    
    private Long _id;
    private String _name; //
    private String _userName; //email
    private String _password;
    private String _host;
    private String _folderName;
    private Integer _connectionTimeout;
    private Integer _readTimeout;
    private Boolean _enabled;
    private String _timezone;
    private List<JEVisObject> _channels;
    
    private Store _store;
    private Folder _folder;
    
    
    private JEVisObject _dataSource;
    private Importer _importer;
    private Parser _parser;
    private List<Result> _result;
    
    

    @Override
    public void run() {
        

        for (JEVisObject channel : _channels) {

            try {
                _result = new ArrayList<Result>();
                JEVisClass parserJevisClass = channel.getDataSource().getJEVisClass(DataCollectorTypes.Parser.NAME);
                JEVisObject parser = channel.getChildren(parserJevisClass, true).get(0);

                _parser = ParserFactory.getParser(parser);
                _parser.initialize(parser);

                List<InputStream> input = this.sendSampleRequest(channel);

                if (!input.isEmpty()) {
                    _parser.parse(input);
                    _result = _parser.getResult();
                }

                if (!_result.isEmpty()) {
                    JEVisImporterAdapter.importResults(_result, _importer, channel);
                }
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(EMailDataSource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        }  
    }

    @Override
    public void initialize(JEVisObject mailObject) {
        _dataSource = mailObject;
        initializeAttributes(mailObject);
        initializeChannelObjects(mailObject);
        
        _importer = ImporterFactory.getImporter(_dataSource);
        if (_importer != null) {
        _importer.initialize(_dataSource);
    }
    }
    
       
    
    @Override
    public List<InputStream> sendSampleRequest(JEVisObject channel){
        List<InputStream> answerList = new ArrayList<InputStream>();
        
        
        
        try {
            Properties props = System.getProperties();
            props.put("mail.imap.host", _host); 
            Session session = Session.getDefaultInstance(props);
            _store = session.getStore();
            _store.connect(_userName, _password);
            if (!_store.isConnected()) {
                org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ERROR, "Connected not possible");
//                throw new FetchingException(_id, FetchingExceptionType.CONNECTION_ERROR);
            }
            _folderName = "INBOX"; //TODO
            _folder = _store.getFolder(_folderName); 

            InputStream answer = null;
            JEVisClass channelClass;
            channelClass = channel.getJEVisClass();
            
            JEVisType senderType = channelClass.getType(DataCollectorTypes.Channel.EMailChannel.SENDER);
            String sender = DatabaseHelper.getObjectAsString(channel, senderType);
            JEVisType subjectType = channelClass.getType(DataCollectorTypes.Channel.EMailChannel.SUBJECT);
            String subject = DatabaseHelper.getObjectAsString(channel, subjectType);
            JEVisType readoutType = channelClass.getType(DataCollectorTypes.Channel.EMailChannel.LAST_READOUT);
            DateTime lastReadout = DatabaseHelper.getObjectAsDate(channel, readoutType, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
//            String filePath = dp.getFilePath();
            org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ALL, "SendSampleRequest");
            
            
            
            SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, lastReadout.toDate());
            SearchTerm senderTerm = new FromTerm(new InternetAddress(sender));
            SearchTerm subjectTerm = new SubjectTerm(subject);        
            SearchTerm andTerm = new AndTerm(newerThan, new AndTerm(senderTerm, subjectTerm)); 
                       
            List<Message> messageList = Arrays.asList(_folder.search(andTerm));
                      
            for (Message message : messageList) {
                //org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ALL, "FileInputName: " + message);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                //org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ALL, "FTPQuery " + message);
                message.writeTo(out);
                //org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ALL, "Request status: " + retrieveFile);
                InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
                answer = new BufferedInputStream(inputStream);
                answerList.add(answer);
            }
            
            _folder.close(false);
            _store.close();
            
        } catch (MessagingException ex) {
            Logger.getLogger(EMailDataSource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JEVisException ex) {
            Logger.getLogger(EMailDataSource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EMailDataSource.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        
        return answerList;
    }

    @Override
    public void parse(List<InputStream> input) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void importResult() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void initializeAttributes(JEVisObject mailObject) {
        try {
            JEVisClass eMailType = mailObject.getDataSource().getJEVisClass(DataCollectorTypes.DataSource.DataServer.EMail.NAME);
            JEVisType hostType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.EMail.HOST);
            JEVisType connectionTimeoutType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.EMail.CONNECTION_TIMEOUT);
            JEVisType readTimeoutType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.EMail.READ_TIMEOUT);
            JEVisType userType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.EMail.USER);
            JEVisType passwordType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.EMail.PASSWORD);
            JEVisType timezoneType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.EMail.TIMEZONE);
            JEVisType enableType = eMailType.getType(DataCollectorTypes.DataSource.DataServer.ENABLE);
            
            _name = mailObject.getName();
            _id = mailObject.getID();
            _host = DatabaseHelper.getObjectAsString(mailObject, hostType);

            _connectionTimeout = DatabaseHelper.getObjectAsInteger(mailObject, connectionTimeoutType);
            _readTimeout = DatabaseHelper.getObjectAsInteger(mailObject, readTimeoutType);

            JEVisAttribute userAttr = mailObject.getAttribute(userType);
            if (!userAttr.hasSample()) {
                _userName = "";
            } else {
                _userName = DatabaseHelper.getObjectAsString(mailObject, userType);
            }

            JEVisAttribute passAttr = mailObject.getAttribute(passwordType);
            if (!passAttr.hasSample()) {
                _password = "";
            } else {
                _password = DatabaseHelper.getObjectAsString(mailObject, passwordType);
            }
            _timezone = DatabaseHelper.getObjectAsString(mailObject, timezoneType);
            _enabled = DatabaseHelper.getObjectAsBoolean(mailObject, enableType);
        } catch (JEVisException ex) {
            Logger.getLogger(EMailDataSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void initializeChannelObjects(JEVisObject mailObject) {
        try {
            JEVisClass channelDirClass = mailObject.getDataSource().getJEVisClass(DataCollectorTypes.ChannelDirectory.EmailChannelDirectory.NAME);
            JEVisObject channelDir = mailObject.getChildren(channelDirClass, false).get(0);
            JEVisClass channelClass = mailObject.getDataSource().getJEVisClass(DataCollectorTypes.Channel.FTPChannel.NAME);
            _channels = channelDir.getChildren(channelClass, false);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(EMailDataSource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    /**public static void main (String[] args) throws JEVisException{
        //initiziale
        EMailDataSource ds = new EMailDataSource();
        JEVisDataSourceSQL client = ds.establishConnection();
        JEVisObject emailObject = client.getObject(1722l);
        ds.initialize(emailObject);
        //run
        ds.run();
    }**/
    
        private JEVisDataSourceSQL establishConnection() throws JEVisException {
        org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ALL, "Connection start");
        JEVisCommandLine cmd = JEVisCommandLine.getInstance();
        String configFile = cmd.getConfigPath();
        org.apache.log4j.Logger.getLogger(this.getClass().getName()).log(org.apache.log4j.Level.ALL, "ConfigFile: " + configFile);

            JEVisDataSourceSQL client = new JEVisDataSourceSQL("openjevis.org", "13306", "jevis", "jevis", "jevistest");
             client.connect("Sys Admin", "JEV34Env");
             return client;
    
    }
}