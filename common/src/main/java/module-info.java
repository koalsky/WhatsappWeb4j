open module it.auties.whatsapp4j.shared {
    exports it.auties.whatsapp4j.common.api;
    exports it.auties.whatsapp4j.common.listener;
    exports it.auties.whatsapp4j.common.utils;
    exports it.auties.whatsapp4j.common.manager;
    exports it.auties.whatsapp4j.common.binary;
    exports it.auties.whatsapp4j.common.protobuf.chat;
    exports it.auties.whatsapp4j.common.protobuf.contact;
    exports it.auties.whatsapp4j.common.protobuf.message.model;
    exports it.auties.whatsapp4j.common.protobuf.message.standard;
    exports it.auties.whatsapp4j.common.protobuf.message.device;
    exports it.auties.whatsapp4j.common.protobuf.message.business;
    exports it.auties.whatsapp4j.common.protobuf.message.server;
    exports it.auties.whatsapp4j.common.protobuf.message.security;
    exports it.auties.whatsapp4j.common.protobuf.info;
    exports it.auties.whatsapp4j.common.protobuf.model.biz;
    exports it.auties.whatsapp4j.common.protobuf.model.button;
    exports it.auties.whatsapp4j.common.protobuf.model.call;
    exports it.auties.whatsapp4j.common.protobuf.model.history;
    exports it.auties.whatsapp4j.common.protobuf.model.hsm;
    exports it.auties.whatsapp4j.common.protobuf.model.media;
    exports it.auties.whatsapp4j.common.protobuf.model.companion;
    exports it.auties.whatsapp4j.common.protobuf.model.setting;
    exports it.auties.whatsapp4j.common.protobuf.model.syncd;
    exports it.auties.whatsapp4j.common.protobuf.model.product;
    exports it.auties.whatsapp4j.common.protobuf.model.key;
    exports it.auties.whatsapp4j.common.protobuf.model.server;
    exports it.auties.whatsapp4j.common.protobuf.model.misc;
    exports it.auties.whatsapp4j.common.protobuf.model.client;
    exports it.auties.whatsapp4j.common.protobuf.model.recent;
    exports it.auties.whatsapp4j.common.serialization;
    exports it.auties.whatsapp4j.common.socket;
    exports it.auties.whatsapp4j.common.protobuf.model.app;
    exports it.auties.whatsapp4j.common.response;
    exports it.auties.whatsapp4j.common.request;

    requires jakarta.websocket;
    requires com.fasterxml.jackson.databind;
    requires com.google.zxing;
    requires java.desktop;
    requires it.auties.protoc.api;
    requires org.bouncycastle.provider;
    requires java.prefs;
    requires java.net.http;
    requires java.compiler;

    requires transitive java.logging;
    requires static lombok;
    requires static jdk.unsupported;
}