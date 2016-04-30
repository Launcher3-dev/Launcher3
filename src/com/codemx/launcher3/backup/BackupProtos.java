package com.codemx.launcher3.backup;

/**
 * Created by yuchuan
 * DATE 16/1/14
 * TIME 21:20
 */
@SuppressWarnings("hiding")
public interface BackupProtos {
    final class Key extends
            com.google.protobuf.nano.MessageNano {
        // enum Type
        public static final int FAVORITE = 1;
        public static final int SCREEN = 2;
        public static final int ICON = 3;
        public static final int WIDGET = 4;
        private static volatile Key[] _emptyArray;

        public static Key[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Key[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required .launcher_backup.Key.Type type = 1;
        public int type;
        // optional string name = 2;
        public java.lang.String name;
        // optional int64 id = 3;
        public long id;
        // optional int64 checksum = 4;
        public long checksum;

        public Key() {
            clear();
        }

        public Key clear() {
            type = BackupProtos.Key.FAVORITE;
            name = "";
            id = 0L;
            checksum = 0L;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeInt32(1, this.type);
            if (!this.name.equals("")) {
                output.writeString(2, this.name);
            }
            if (this.id != 0L) {
                output.writeInt64(3, this.id);
            }
            if (this.checksum != 0L) {
                output.writeInt64(4, this.checksum);
            }
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt32Size(1, this.type);
            if (!this.name.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(2, this.name);
            }
            if (this.id != 0L) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt64Size(3, this.id);
            }
            if (this.checksum != 0L) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt64Size(4, this.checksum);
            }
            return size;
        }

        @Override
        public Key mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 8: {
                        int value = input.readInt32();
                        switch (value) {
                            case BackupProtos.Key.FAVORITE:
                            case BackupProtos.Key.SCREEN:
                            case BackupProtos.Key.ICON:
                            case BackupProtos.Key.WIDGET:
                                this.type = value;
                                break;
                        }
                        break;
                    }
                    case 18: {
                        this.name = input.readString();
                        break;
                    }
                    case 24: {
                        this.id = input.readInt64();
                        break;
                    }
                    case 32: {
                        this.checksum = input.readInt64();
                        break;
                    }
                }
            }
        }

        public static Key parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new Key(), data);
        }

        public static Key parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new Key().mergeFrom(input);
        }
    }

    final class CheckedMessage extends
            com.google.protobuf.nano.MessageNano {
        private static volatile CheckedMessage[] _emptyArray;

        public static CheckedMessage[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new CheckedMessage[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required bytes payload = 1;
        public byte[] payload;
        // required int64 checksum = 2;
        public long checksum;

        public CheckedMessage() {
            clear();
        }

        public CheckedMessage clear() {
            payload = com.google.protobuf.nano.WireFormatNano.EMPTY_BYTES;
            checksum = 0L;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeBytes(1, this.payload);
            output.writeInt64(2, this.checksum);
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeBytesSize(1, this.payload);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt64Size(2, this.checksum);
            return size;
        }

        @Override
        public CheckedMessage mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 10: {
                        this.payload = input.readBytes();
                        break;
                    }
                    case 16: {
                        this.checksum = input.readInt64();
                        break;
                    }
                }
            }
        }

        public static CheckedMessage parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new CheckedMessage(), data);
        }

        public static CheckedMessage parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new CheckedMessage().mergeFrom(input);
        }
    }

    final class DeviceProfieData extends
            com.google.protobuf.nano.MessageNano {
        private static volatile DeviceProfieData[] _emptyArray;

        public static DeviceProfieData[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new DeviceProfieData[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required float desktop_rows = 1;
        public float desktopRows;
        // required float desktop_cols = 2;
        public float desktopCols;
        // required float hotseat_count = 3;
        public float hotseatCount;
        // required int32 allapps_rank = 4;
        public int allappsRank;

        public DeviceProfieData() {
            clear();
        }

        public DeviceProfieData clear() {
            desktopRows = 0F;
            desktopCols = 0F;
            hotseatCount = 0F;
            allappsRank = 0;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeFloat(1, this.desktopRows);
            output.writeFloat(2, this.desktopCols);
            output.writeFloat(3, this.hotseatCount);
            output.writeInt32(4, this.allappsRank);
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeFloatSize(1, this.desktopRows);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeFloatSize(2, this.desktopCols);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeFloatSize(3, this.hotseatCount);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt32Size(4, this.allappsRank);
            return size;
        }

        @Override
        public DeviceProfieData mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 13: {
                        this.desktopRows = input.readFloat();
                        break;
                    }
                    case 21: {
                        this.desktopCols = input.readFloat();
                        break;
                    }
                    case 29: {
                        this.hotseatCount = input.readFloat();
                        break;
                    }
                    case 32: {
                        this.allappsRank = input.readInt32();
                        break;
                    }
                }
            }
        }

        public static DeviceProfieData parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new DeviceProfieData(), data);
        }

        public static DeviceProfieData parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new DeviceProfieData().mergeFrom(input);
        }
    }

    final class Journal extends
            com.google.protobuf.nano.MessageNano {
        private static volatile Journal[] _emptyArray;

        public static Journal[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Journal[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required int32 app_version = 1;
        public int appVersion;
        // required int64 t = 2;
        public long t;
        // optional int64 bytes = 3;
        public long bytes;
        // optional int32 rows = 4;
        public int rows;
        // repeated .launcher_backup.Key key = 5;
        public BackupProtos.Key[] key;
        // optional int32 backup_version = 6 [default = 1];
        public int backupVersion;
        // optional .launcher_backup.DeviceProfieData profile = 7;
        public BackupProtos.DeviceProfieData profile;

        public Journal() {
            clear();
        }

        public Journal clear() {
            appVersion = 0;
            t = 0L;
            bytes = 0L;
            rows = 0;
            key = BackupProtos.Key.emptyArray();
            backupVersion = 1;
            profile = null;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeInt32(1, this.appVersion);
            output.writeInt64(2, this.t);
            if (this.bytes != 0L) {
                output.writeInt64(3, this.bytes);
            }
            if (this.rows != 0) {
                output.writeInt32(4, this.rows);
            }
            if (this.key != null && this.key.length > 0) {
                for (int i = 0; i < this.key.length; i++) {
                    BackupProtos.Key element = this.key[i];
                    if (element != null) {
                        output.writeMessage(5, element);
                    }
                }
            }
            if (this.backupVersion != 1) {
                output.writeInt32(6, this.backupVersion);
            }
            if (this.profile != null) {
                output.writeMessage(7, this.profile);
            }
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt32Size(1, this.appVersion);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt64Size(2, this.t);
            if (this.bytes != 0L) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt64Size(3, this.bytes);
            }
            if (this.rows != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(4, this.rows);
            }
            if (this.key != null && this.key.length > 0) {
                for (int i = 0; i < this.key.length; i++) {
                    BackupProtos.Key element = this.key[i];
                    if (element != null) {
                        size += com.google.protobuf.nano.CodedOutputByteBufferNano
                                .computeMessageSize(5, element);
                    }
                }
            }
            if (this.backupVersion != 1) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(6, this.backupVersion);
            }
            if (this.profile != null) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeMessageSize(7, this.profile);
            }
            return size;
        }

        @Override
        public Journal mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 8: {
                        this.appVersion = input.readInt32();
                        break;
                    }
                    case 16: {
                        this.t = input.readInt64();
                        break;
                    }
                    case 24: {
                        this.bytes = input.readInt64();
                        break;
                    }
                    case 32: {
                        this.rows = input.readInt32();
                        break;
                    }
                    case 42: {
                        int arrayLength = com.google.protobuf.nano.WireFormatNano
                                .getRepeatedFieldArrayLength(input, 42);
                        int i = this.key == null ? 0 : this.key.length;
                        BackupProtos.Key[] newArray =
                                new BackupProtos.Key[i + arrayLength];
                        if (i != 0) {
                            java.lang.System.arraycopy(this.key, 0, newArray, 0, i);
                        }
                        for (; i < newArray.length - 1; i++) {
                            newArray[i] = new BackupProtos.Key();
                            input.readMessage(newArray[i]);
                            input.readTag();
                        }
                        // Last one without readTag.
                        newArray[i] = new BackupProtos.Key();
                        input.readMessage(newArray[i]);
                        this.key = newArray;
                        break;
                    }
                    case 48: {
                        this.backupVersion = input.readInt32();
                        break;
                    }
                    case 58: {
                        if (this.profile == null) {
                            this.profile = new BackupProtos.DeviceProfieData();
                        }
                        input.readMessage(this.profile);
                        break;
                    }
                }
            }
        }

        public static Journal parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new Journal(), data);
        }

        public static Journal parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new Journal().mergeFrom(input);
        }
    }

    final class Favorite extends
            com.google.protobuf.nano.MessageNano {
        // enum TargetType
        public static final int TARGET_NONE = 0;
        public static final int TARGET_PHONE = 1;
        public static final int TARGET_MESSENGER = 2;
        public static final int TARGET_EMAIL = 3;
        public static final int TARGET_BROWSER = 4;
        public static final int TARGET_GALLERY = 5;
        public static final int TARGET_CAMERA = 6;
        private static volatile Favorite[] _emptyArray;

        public static Favorite[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Favorite[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required int64 id = 1;
        public long id;
        // required int32 itemType = 2;
        public int itemType;
        // optional string title = 3;
        public java.lang.String title;
        // optional int32 container = 4;
        public int container;
        // optional int32 screen = 5;
        public int screen;
        // optional int32 cellX = 6;
        public int cellX;
        // optional int32 cellY = 7;
        public int cellY;
        // optional int32 spanX = 8;
        public int spanX;
        // optional int32 spanY = 9;
        public int spanY;
        // optional int32 displayMode = 10;
        public int displayMode;
        // optional int32 appWidgetId = 11;
        public int appWidgetId;
        // optional string appWidgetProvider = 12;
        public java.lang.String appWidgetProvider;
        // optional string intent = 13;
        public java.lang.String intent;
        // optional string uri = 14;
        public java.lang.String uri;
        // optional int32 iconType = 15;
        public int iconType;
        // optional string iconPackage = 16;
        public java.lang.String iconPackage;
        // optional string iconResource = 17;
        public java.lang.String iconResource;
        // optional bytes icon = 18;
        public byte[] icon;
        // optional .launcher_backup.Favorite.TargetType targetType = 19 [default = TARGET_NONE];
        public int targetType;
        // optional int32 rank = 20;
        public int rank;

        public Favorite() {
            clear();
        }

        public Favorite clear() {
            id = 0L;
            itemType = 0;
            title = "";
            container = 0;
            screen = 0;
            cellX = 0;
            cellY = 0;
            spanX = 0;
            spanY = 0;
            displayMode = 0;
            appWidgetId = 0;
            appWidgetProvider = "";
            intent = "";
            uri = "";
            iconType = 0;
            iconPackage = "";
            iconResource = "";
            icon = com.google.protobuf.nano.WireFormatNano.EMPTY_BYTES;
            targetType = BackupProtos.Favorite.TARGET_NONE;
            rank = 0;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeInt64(1, this.id);
            output.writeInt32(2, this.itemType);
            if (!this.title.equals("")) {
                output.writeString(3, this.title);
            }
            if (this.container != 0) {
                output.writeInt32(4, this.container);
            }
            if (this.screen != 0) {
                output.writeInt32(5, this.screen);
            }
            if (this.cellX != 0) {
                output.writeInt32(6, this.cellX);
            }
            if (this.cellY != 0) {
                output.writeInt32(7, this.cellY);
            }
            if (this.spanX != 0) {
                output.writeInt32(8, this.spanX);
            }
            if (this.spanY != 0) {
                output.writeInt32(9, this.spanY);
            }
            if (this.displayMode != 0) {
                output.writeInt32(10, this.displayMode);
            }
            if (this.appWidgetId != 0) {
                output.writeInt32(11, this.appWidgetId);
            }
            if (!this.appWidgetProvider.equals("")) {
                output.writeString(12, this.appWidgetProvider);
            }
            if (!this.intent.equals("")) {
                output.writeString(13, this.intent);
            }
            if (!this.uri.equals("")) {
                output.writeString(14, this.uri);
            }
            if (this.iconType != 0) {
                output.writeInt32(15, this.iconType);
            }
            if (!this.iconPackage.equals("")) {
                output.writeString(16, this.iconPackage);
            }
            if (!this.iconResource.equals("")) {
                output.writeString(17, this.iconResource);
            }
            if (!java.util.Arrays.equals(this.icon, com.google.protobuf.nano.WireFormatNano.EMPTY_BYTES)) {
                output.writeBytes(18, this.icon);
            }
            if (this.targetType != BackupProtos.Favorite.TARGET_NONE) {
                output.writeInt32(19, this.targetType);
            }
            if (this.rank != 0) {
                output.writeInt32(20, this.rank);
            }
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt64Size(1, this.id);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt32Size(2, this.itemType);
            if (!this.title.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(3, this.title);
            }
            if (this.container != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(4, this.container);
            }
            if (this.screen != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(5, this.screen);
            }
            if (this.cellX != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(6, this.cellX);
            }
            if (this.cellY != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(7, this.cellY);
            }
            if (this.spanX != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(8, this.spanX);
            }
            if (this.spanY != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(9, this.spanY);
            }
            if (this.displayMode != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(10, this.displayMode);
            }
            if (this.appWidgetId != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(11, this.appWidgetId);
            }
            if (!this.appWidgetProvider.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(12, this.appWidgetProvider);
            }
            if (!this.intent.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(13, this.intent);
            }
            if (!this.uri.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(14, this.uri);
            }
            if (this.iconType != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(15, this.iconType);
            }
            if (!this.iconPackage.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(16, this.iconPackage);
            }
            if (!this.iconResource.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(17, this.iconResource);
            }
            if (!java.util.Arrays.equals(this.icon, com.google.protobuf.nano.WireFormatNano.EMPTY_BYTES)) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeBytesSize(18, this.icon);
            }
            if (this.targetType != BackupProtos.Favorite.TARGET_NONE) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(19, this.targetType);
            }
            if (this.rank != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(20, this.rank);
            }
            return size;
        }

        @Override
        public Favorite mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 8: {
                        this.id = input.readInt64();
                        break;
                    }
                    case 16: {
                        this.itemType = input.readInt32();
                        break;
                    }
                    case 26: {
                        this.title = input.readString();
                        break;
                    }
                    case 32: {
                        this.container = input.readInt32();
                        break;
                    }
                    case 40: {
                        this.screen = input.readInt32();
                        break;
                    }
                    case 48: {
                        this.cellX = input.readInt32();
                        break;
                    }
                    case 56: {
                        this.cellY = input.readInt32();
                        break;
                    }
                    case 64: {
                        this.spanX = input.readInt32();
                        break;
                    }
                    case 72: {
                        this.spanY = input.readInt32();
                        break;
                    }
                    case 80: {
                        this.displayMode = input.readInt32();
                        break;
                    }
                    case 88: {
                        this.appWidgetId = input.readInt32();
                        break;
                    }
                    case 98: {
                        this.appWidgetProvider = input.readString();
                        break;
                    }
                    case 106: {
                        this.intent = input.readString();
                        break;
                    }
                    case 114: {
                        this.uri = input.readString();
                        break;
                    }
                    case 120: {
                        this.iconType = input.readInt32();
                        break;
                    }
                    case 130: {
                        this.iconPackage = input.readString();
                        break;
                    }
                    case 138: {
                        this.iconResource = input.readString();
                        break;
                    }
                    case 146: {
                        this.icon = input.readBytes();
                        break;
                    }
                    case 152: {
                        int value = input.readInt32();
                        switch (value) {
                            case BackupProtos.Favorite.TARGET_NONE:
                            case BackupProtos.Favorite.TARGET_PHONE:
                            case BackupProtos.Favorite.TARGET_MESSENGER:
                            case BackupProtos.Favorite.TARGET_EMAIL:
                            case BackupProtos.Favorite.TARGET_BROWSER:
                            case BackupProtos.Favorite.TARGET_GALLERY:
                            case BackupProtos.Favorite.TARGET_CAMERA:
                                this.targetType = value;
                                break;
                        }
                        break;
                    }
                    case 160: {
                        this.rank = input.readInt32();
                        break;
                    }
                }
            }
        }

        public static Favorite parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new Favorite(), data);
        }

        public static Favorite parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new Favorite().mergeFrom(input);
        }
    }

    final class Screen extends
            com.google.protobuf.nano.MessageNano {
        private static volatile Screen[] _emptyArray;

        public static Screen[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Screen[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required int64 id = 1;
        public long id;
        // optional int32 rank = 2;
        public int rank;

        public Screen() {
            clear();
        }

        public Screen clear() {
            id = 0L;
            rank = 0;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeInt64(1, this.id);
            if (this.rank != 0) {
                output.writeInt32(2, this.rank);
            }
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt64Size(1, this.id);
            if (this.rank != 0) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(2, this.rank);
            }
            return size;
        }

        @Override
        public Screen mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 8: {
                        this.id = input.readInt64();
                        break;
                    }
                    case 16: {
                        this.rank = input.readInt32();
                        break;
                    }
                }
            }
        }

        public static Screen parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new Screen(), data);
        }

        public static Screen parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new Screen().mergeFrom(input);
        }
    }

    final class Resource extends
            com.google.protobuf.nano.MessageNano {
        private static volatile Resource[] _emptyArray;

        public static Resource[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Resource[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required int32 dpi = 1;
        public int dpi;
        // required bytes data = 2;
        public byte[] data;

        public Resource() {
            clear();
        }

        public Resource clear() {
            dpi = 0;
            data = com.google.protobuf.nano.WireFormatNano.EMPTY_BYTES;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeInt32(1, this.dpi);
            output.writeBytes(2, this.data);
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeInt32Size(1, this.dpi);
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeBytesSize(2, this.data);
            return size;
        }

        @Override
        public Resource mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 8: {
                        this.dpi = input.readInt32();
                        break;
                    }
                    case 18: {
                        this.data = input.readBytes();
                        break;
                    }
                }
            }
        }

        public static Resource parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new Resource(), data);
        }

        public static Resource parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new Resource().mergeFrom(input);
        }
    }

    final class Widget extends
            com.google.protobuf.nano.MessageNano {
        private static volatile Widget[] _emptyArray;

        public static Widget[] emptyArray() {
            // Lazily initializes the empty array
            if (_emptyArray == null) {
                synchronized (
                        com.google.protobuf.nano.InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Widget[0];
                    }
                }
            }
            return _emptyArray;
        }

        // required string provider = 1;
        public java.lang.String provider;
        // optional string label = 2;
        public java.lang.String label;
        // optional bool configure = 3;
        public boolean configure;
        // optional .launcher_backup.Resource icon = 4;
        public BackupProtos.Resource icon;
        // optional .launcher_backup.Resource preview = 5;
        public BackupProtos.Resource preview;
        // optional int32 minSpanX = 6 [default = 2];
        public int minSpanX;
        // optional int32 minSpanY = 7 [default = 2];
        public int minSpanY;

        public Widget() {
            clear();
        }

        public Widget clear() {
            provider = "";
            label = "";
            configure = false;
            icon = null;
            preview = null;
            minSpanX = 2;
            minSpanY = 2;
            cachedSize = -1;
            return this;
        }

        @Override
        public void writeTo(com.google.protobuf.nano.CodedOutputByteBufferNano output)
                throws java.io.IOException {
            output.writeString(1, this.provider);
            if (!this.label.equals("")) {
                output.writeString(2, this.label);
            }
            if (this.configure != false) {
                output.writeBool(3, this.configure);
            }
            if (this.icon != null) {
                output.writeMessage(4, this.icon);
            }
            if (this.preview != null) {
                output.writeMessage(5, this.preview);
            }
            if (this.minSpanX != 2) {
                output.writeInt32(6, this.minSpanX);
            }
            if (this.minSpanY != 2) {
                output.writeInt32(7, this.minSpanY);
            }
            super.writeTo(output);
        }

        @Override
        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            size += com.google.protobuf.nano.CodedOutputByteBufferNano
                    .computeStringSize(1, this.provider);
            if (!this.label.equals("")) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeStringSize(2, this.label);
            }
            if (this.configure != false) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeBoolSize(3, this.configure);
            }
            if (this.icon != null) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeMessageSize(4, this.icon);
            }
            if (this.preview != null) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeMessageSize(5, this.preview);
            }
            if (this.minSpanX != 2) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(6, this.minSpanX);
            }
            if (this.minSpanY != 2) {
                size += com.google.protobuf.nano.CodedOutputByteBufferNano
                        .computeInt32Size(7, this.minSpanY);
            }
            return size;
        }

        @Override
        public Widget mergeFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    default: {
                        if (!com.google.protobuf.nano.WireFormatNano.parseUnknownField(input, tag)) {
                            return this;
                        }
                        break;
                    }
                    case 10: {
                        this.provider = input.readString();
                        break;
                    }
                    case 18: {
                        this.label = input.readString();
                        break;
                    }
                    case 24: {
                        this.configure = input.readBool();
                        break;
                    }
                    case 34: {
                        if (this.icon == null) {
                            this.icon = new BackupProtos.Resource();
                        }
                        input.readMessage(this.icon);
                        break;
                    }
                    case 42: {
                        if (this.preview == null) {
                            this.preview = new BackupProtos.Resource();
                        }
                        input.readMessage(this.preview);
                        break;
                    }
                    case 48: {
                        this.minSpanX = input.readInt32();
                        break;
                    }
                    case 56: {
                        this.minSpanY = input.readInt32();
                        break;
                    }
                }
            }
        }

        public static Widget parseFrom(byte[] data)
                throws com.google.protobuf.nano.InvalidProtocolBufferNanoException {
            return com.google.protobuf.nano.MessageNano.mergeFrom(new Widget(), data);
        }

        public static Widget parseFrom(
                com.google.protobuf.nano.CodedInputByteBufferNano input)
                throws java.io.IOException {
            return new Widget().mergeFrom(input);
        }
    }
}
