#! /usr/bin/env groovy

@Grapes([
        @Grab(group='javax.mail', module='mail', version='1.4.7'),
        @Grab(group='javax.activation', module='activation', version='1.1.1')
])

import javax.mail.internet.*;
import javax.mail.*
import javax.activation.*


def sendMail(subject, message, configFile){

    def configObject = new ConfigSlurper().parse(configFile.text)

    def session = Session.getDefaultInstance(configObject.toProperties(),new javax.mail.Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(configObject.toProperties().get("mail.smtp.user"), configObject.toProperties().get("mail.smtp.password"));
        }
    });

    def msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(configObject.toProperties().get("mail.smtp.user")));

    def sendTo = configObject.toProperties().get("mail.receiptor").split(";").collect{
        new InternetAddress(it);
    } as InternetAddress[]

    msg.setRecipients(MimeMessage.RecipientType.TO,sendTo);
    msg.setSubject(subject);
    msg.setText(message)
    Transport.send(msg);
}
