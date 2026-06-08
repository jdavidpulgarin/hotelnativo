module com.hotel {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    // BCrypt para hashing seguro de contraseñas (org.mindrot:jbcrypt:0.4)
    requires jbcrypt;
    // OpenPDF para generación de reportes en formato PDF
    requires com.github.librepdf.openpdf;
    // Jakarta Mail (angus-mail) para envío de correos SMTP
    requires org.eclipse.angus.mail;

    opens com.hotel to javafx.fxml;
    opens com.hotel.ui.controllers to javafx.fxml;
    opens com.hotel.ui.components  to javafx.fxml;
    opens com.hotel.model          to javafx.base;
    opens com.hotel.view           to javafx.fxml;

    exports com.hotel;
    exports com.hotel.ui.controllers;
    exports com.hotel.ui.components;
    exports com.hotel.model;
    exports com.hotel.service;
    exports com.hotel.service.strategy;
    exports com.hotel.dao.impl;
    exports com.hotel.dao.interfaces;
    exports com.hotel.dto;
    exports com.hotel.exception;
    exports com.hotel.util;
}
