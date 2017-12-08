#! /usr/bin/env groovy


@Grab(group = 'javax.mail', module = 'mail', version = '1.4.7')
@GrabConfig(systemClassLoader=true, initContextClassLoader=true)
@Grab(group = 'javax.activation', module = 'activation', version = '1.1.1')
import javax.mail.internet.*;
import javax.mail.*
import javax.activation.*


def sendMail(subject, message, configFile, attachment = null) {

    def configObject = new ConfigSlurper().parse(configFile.text)

    def session = Session.getDefaultInstance(configObject.toProperties(), new javax.mail.Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(configObject.toProperties().get("mail.smtp.user"), configObject.toProperties().get("mail.smtp.password"));
        }
    });

    def mimeMessage = new MimeMessage(session);

    mimeMessage.setFrom(new InternetAddress(configObject.toProperties().get("mail.smtp.user")));

    def sendTo = configObject.toProperties().get("mail.receiptor").split(";").collect {
        new InternetAddress(it);
    } as InternetAddress[]

    mimeMessage.setRecipients(MimeMessage.RecipientType.TO, sendTo);
    mimeMessage.setSubject(subject);

    // Create the message part
    def messageBodyPart = new MimeBodyPart();
    // Now set the actual message
    messageBodyPart.setText(message);
    // Create a multipar message
    Multipart multipart = new MimeMultipart();
    // Set text message part
    multipart.addBodyPart(messageBodyPart);

    if (attachment) {
        // Part two is attachment
        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(attachment);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(attachment);
        multipart.addBodyPart(messageBodyPart);
        // Send the complete message parts
    }

    mimeMessage.setContent(multipart);
    Transport.send(mimeMessage);
}
