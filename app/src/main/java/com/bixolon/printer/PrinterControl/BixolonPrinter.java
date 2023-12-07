package com.bixolon.printer.PrinterControl;

import android.content.Context;
import android.graphics.Bitmap;

import com.bxl.config.editor.BXLConfigLoader;

import java.nio.ByteBuffer;

import jpos.JposException;
import jpos.POSPrinter;
import jpos.POSPrinterConst;
import jpos.config.JposEntry;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.DirectIOEvent;
import jpos.events.DirectIOListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;

public class BixolonPrinter implements ErrorListener, OutputCompleteListener, StatusUpdateListener, DirectIOListener, DataListener {
    // ------------------- alignment ------------------- //
    public static int ALIGNMENT_LEFT = 1;
    public static int ALIGNMENT_CENTER = 2;
    public static int ALIGNMENT_RIGHT = 4;

    // ------------------- Text attribute ------------------- //
    public static int ATTRIBUTE_NORMAL = 0;
    public static int ATTRIBUTE_FONT_A = 1;
    public static int ATTRIBUTE_FONT_B = 2;
    public static int ATTRIBUTE_FONT_C = 4;
    public static int ATTRIBUTE_BOLD = 8;
    public static int ATTRIBUTE_UNDERLINE = 16;
    public static int ATTRIBUTE_REVERSE = 32;
    public static int ATTRIBUTE_FONT_D = 64;

    // ------------------- Barcode Symbology ------------------- //
    public static int BARCODE_TYPE_UPCA = POSPrinterConst.PTR_BCS_UPCA;
    public static int BARCODE_TYPE_UPCE = POSPrinterConst.PTR_BCS_UPCE;
    public static int BARCODE_TYPE_EAN8 = POSPrinterConst.PTR_BCS_EAN8;
    public static int BARCODE_TYPE_EAN13 = POSPrinterConst.PTR_BCS_EAN13;
    public static int BARCODE_TYPE_ITF = POSPrinterConst.PTR_BCS_ITF;
    public static int BARCODE_TYPE_Codabar = POSPrinterConst.PTR_BCS_Codabar;
    public static int BARCODE_TYPE_Code39 = POSPrinterConst.PTR_BCS_Code39;
    public static int BARCODE_TYPE_Code93 = POSPrinterConst.PTR_BCS_Code93;
    public static int BARCODE_TYPE_Code128 = POSPrinterConst.PTR_BCS_Code128;
    public static int BARCODE_TYPE_PDF417 = POSPrinterConst.PTR_BCS_PDF417;
    public static int BARCODE_TYPE_MAXICODE = POSPrinterConst.PTR_BCS_MAXICODE;
    public static int BARCODE_TYPE_DATAMATRIX = POSPrinterConst.PTR_BCS_DATAMATRIX;
    public static int BARCODE_TYPE_QRCODE = POSPrinterConst.PTR_BCS_QRCODE;
    public static int BARCODE_TYPE_EAN128 = POSPrinterConst.PTR_BCS_EAN128;

    // ------------------- Barcode HRI ------------------- //
    public static int BARCODE_HRI_NONE = POSPrinterConst.PTR_BC_TEXT_NONE;
    public static int BARCODE_HRI_ABOVE = POSPrinterConst.PTR_BC_TEXT_ABOVE;
    public static int BARCODE_HRI_BELOW = POSPrinterConst.PTR_BC_TEXT_BELOW;

    // ------------------- Farsi Option ------------------- //
    public static int OPT_REORDER_FARSI_RTL = 0;
    public static int OPT_REORDER_FARSI_MIXED = 1;

    // ------------------- CharacterSet ------------------- //
    public static int CS_437_USA_STANDARD_EUROPE = 437;
    public static int CS_737_GREEK = 737;
    public static int CS_775_BALTIC = 775;
    public static int CS_850_MULTILINGUAL = 850;
    public static int CS_852_LATIN2 = 852;
    public static int CS_855_CYRILLIC = 855;
    public static int CS_857_TURKISH = 857;
    public static int CS_858_EURO = 858;
    public static int CS_860_PORTUGUESE = 860;
    public static int CS_862_HEBREW_DOS_CODE = 862;
    public static int CS_863_CANADIAN_FRENCH = 863;
    public static int CS_864_ARABIC = 864;
    public static int CS_865_NORDIC = 865;
    public static int CS_866_CYRILLIC2 = 866;
    public static int CS_928_GREEK = 928;
    public static int CS_1250_CZECH = 1250;
    public static int CS_1251_CYRILLIC = 1251;
    public static int CS_1252_LATIN1 = 1252;
    public static int CS_1253_GREEK = 1253;
    public static int CS_1254_TURKISH = 1254;
    public static int CS_1255_HEBREW_NEW_CODE = 1255;
    public static int CS_1256_ARABIC = 1256;
    public static int CS_1257_BALTIC = 1257;
    public static int CS_1258_VIETNAM = 1258;
    public static int CS_FARSI = 7065;
    public static int CS_KATAKANA = 7565;
    public static int CS_KHMER_CAMBODIA = 7572;
    public static int CS_THAI11 = 8411;
    public static int CS_THAI14 = 8414;
    public static int CS_THAI16 = 8416;
    public static int CS_THAI18 = 8418;
    public static int CS_THAI42 = 8442;
    public static int CS_KS5601 = 5601;
    public static int CS_BIG5 = 6605;
    public static int CS_GB2312 = 2312;
    public static int CS_SHIFT_JIS = 8374;
    public static int CS_TCVN_3_1 = 3031;
    public static int CS_TCVN_3_2 = 3032;

    private Context context = null;

    private BXLConfigLoader bxlConfigLoader = null;
    private POSPrinter posPrinter = null;

    public BixolonPrinter(Context context) {
        this.context = context;

        posPrinter = new POSPrinter(this.context);

        bxlConfigLoader = new BXLConfigLoader(this.context);
        try {
            bxlConfigLoader.openFile();
        } catch (Exception e) {
            bxlConfigLoader.newFile();
        }

    }

    public boolean printerOpen(int portType, String logicalName, String address, boolean isAsyncMode) {
        if (setTargetDevice(portType, logicalName, BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER, address)) {
            int retry = 1;
            if (portType == BXLConfigLoader.DEVICE_BUS_BLUETOOTH_LE) {
                retry = 5;
            }

            for (int i = 0; i < retry; i++) {
                try {
                    posPrinter.open(logicalName);
                    posPrinter.claim(5000 * 2);
                    posPrinter.setDeviceEnabled(true);
                    posPrinter.setAsyncMode(isAsyncMode);

                    return true;
                } catch (JposException e) {
                    e.printStackTrace();
                    try {
                        posPrinter.close();
                    } catch (JposException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return false;
    }

    public boolean printerClose() {
        try {
            if (posPrinter.getClaimed()) {
                posPrinter.setDeviceEnabled(false);
                posPrinter.release();
                posPrinter.close();
            }
        } catch (JposException e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean setTargetDevice(int portType, String logicalName, int deviceCategory, String address) {
        try {
            for (Object entry : bxlConfigLoader.getEntries()) {
                JposEntry jposEntry = (JposEntry) entry;
                if (jposEntry.getLogicalName().equals(logicalName)) {
                    bxlConfigLoader.removeEntry(logicalName);
                    break;
                }
            }

            bxlConfigLoader.addEntry(logicalName, deviceCategory, getProductName(logicalName), portType, address);

            bxlConfigLoader.saveFile();
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
        return true;
    }

    private String getProductName(String name) {
        String productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200II;

        if ((name.equals("SPP-R200III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200III;
        } else if ((name.equals("SPP-R210"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R210;
        } else if ((name.equals("SPP-R215"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R215;
        } else if ((name.equals("SPP-R220"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R220;
        } else if ((name.equals("SPP-C200"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_C200;
        } else if ((name.equals("SPP-R300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R300;
        } else if ((name.equals("SPP-R310"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310;
        } else if ((name.equals("SPP-R318"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R318;
        } else if ((name.equals("SPP-C300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_C300;
        } else if ((name.equals("SPP-R400"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R400;
        } else if ((name.equals("SPP-R410"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R410;
        } else if ((name.equals("SPP-R418"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R418;
        } else if ((name.equals("SPP-100II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_100II;
        } else if ((name.equals("SRP-350IIOBE"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350IIOBE;
        } else if ((name.equals("SRP-350III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350III;
        } else if ((name.equals("SRP-352III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352III;
        } else if ((name.equals("SRP-350V"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350V;
        } else if ((name.equals("SRP-352V"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352V;
        } else if ((name.equals("SRP-350plusIII"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350PLUSIII;
        } else if ((name.equals("SRP-352plusIII"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352PLUSIII;
        } else if ((name.equals("SRP-350plusV"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_350PLUSV;
        } else if ((name.equals("SRP-352plusV"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_352PLUSV;
        } else if ((name.equals("SRP-380"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_380;
        } else if ((name.equals("SRP-382"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_382;
        } else if ((name.equals("SRP-383"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_383;
        } else if ((name.equals("SRP-380II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_380II;
        } else if ((name.equals("SRP-382II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_382II;
        } else if ((name.equals("SRP-340II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_340II;
        } else if ((name.equals("SRP-342II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_342II;
        } else if ((name.equals("SRP-Q200"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q200;
        } else if ((name.equals("SRP-Q300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q300;
        } else if ((name.equals("SRP-Q302"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_Q302;
        } else if ((name.equals("SRP-QE300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE300;
        } else if ((name.equals("SRP-QE302"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_QE302;
        } else if ((name.equals("SRP-E300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_E300;
        } else if ((name.equals("SRP-E302"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_E302;
        } else if ((name.equals("SRP-B300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_B300;
        } else if ((name.equals("SRP-330II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_330II;
        } else if ((name.equals("SRP-332II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_332II;
        } else if ((name.equals("SRP-330III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_330III;
        } else if ((name.equals("SRP-332III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_332III;
        } else if ((name.equals("SRP-S200"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S200;
        } else if ((name.equals("SRP-S300"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S300;
        } else if ((name.equals("SRP-S320"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S320;
        } else if ((name.equals("SRP-S3000"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_S3000;
        } else if ((name.equals("SRP-F310"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310;
        } else if ((name.equals("SRP-F312"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312;
        } else if ((name.equals("SRP-F310II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F310II;
        } else if ((name.equals("SRP-F312II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F312II;
        } else if ((name.equals("SRP-F313II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_F313II;
        } else if ((name.equals("SRP-275III"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SRP_275III;
        } else if ((name.equals("BK3-2"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_BK3_2;
        } else if ((name.equals("BK3-3"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_BK3_3;
        } else if ((name.equals("BK5-3"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_BK5_3;
        } else if ((name.equals("SMB6350"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SMB6350;
        } else if ((name.equals("SLP X-Series"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SLP_X_SERIES;
        } else if ((name.equals("SLP-DX420"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SLP_DX420;
        } else if ((name.equals("SPP-L410II"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_L410II;
        } else if ((name.equals("XM7-40"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_XM7_40;
        } else if ((name.equals("MSR"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_MSR;
        } else if ((name.equals("CashDrawer"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_CASH_DRAWER;
        } else if ((name.equals("LocalSmartCardRW"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_LOCAL_SMART_CARD_RW;
        } else if ((name.equals("SmartCardRW"))) {
            productName = BXLConfigLoader.PRODUCT_NAME_SMART_CARD_RW;
        }

        return productName;
    }

    public boolean printText(String data, int alignment, int attribute, int textSize) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            String strOption = EscapeSequence.getString(0);

            if ((alignment & ALIGNMENT_LEFT) == ALIGNMENT_LEFT) {
                strOption += EscapeSequence.getString(4);
            }

            if ((alignment & ALIGNMENT_CENTER) == ALIGNMENT_CENTER) {
                strOption += EscapeSequence.getString(5);
            }

            if ((alignment & ALIGNMENT_RIGHT) == ALIGNMENT_RIGHT) {
                strOption += EscapeSequence.getString(6);
            }

            if ((attribute & ATTRIBUTE_FONT_A) == ATTRIBUTE_FONT_A) {
                strOption += EscapeSequence.getString(1);
            }

            if ((attribute & ATTRIBUTE_FONT_B) == ATTRIBUTE_FONT_B) {
                strOption += EscapeSequence.getString(2);
            }

            if ((attribute & ATTRIBUTE_FONT_C) == ATTRIBUTE_FONT_C) {
                strOption += EscapeSequence.getString(3);
            }

            if ((attribute & ATTRIBUTE_FONT_D) == ATTRIBUTE_FONT_D) {
                strOption += EscapeSequence.getString(33);
            }

            if ((attribute & ATTRIBUTE_BOLD) == ATTRIBUTE_BOLD) {
                strOption += EscapeSequence.getString(7);
            }

            if ((attribute & ATTRIBUTE_UNDERLINE) == ATTRIBUTE_UNDERLINE) {
                strOption += EscapeSequence.getString(9);
            }

            if ((attribute & ATTRIBUTE_REVERSE) == ATTRIBUTE_REVERSE) {
                strOption += EscapeSequence.getString(11);
            }

            switch (textSize) {
                case 1:
                    strOption += EscapeSequence.getString(17);
                    strOption += EscapeSequence.getString(25);
                    break;
                case 2:
                    strOption += EscapeSequence.getString(15);
                    strOption += EscapeSequence.getString(26);
                    break;
                case 3:
                    strOption += EscapeSequence.getString(19);
                    strOption += EscapeSequence.getString(27);
                    break;
                case 4:
                    strOption += EscapeSequence.getString(20);
                    strOption += EscapeSequence.getString(28);
                    break;
                case 5:
                    strOption += EscapeSequence.getString(21);
                    strOption += EscapeSequence.getString(29);
                    break;
                case 6:
                    strOption += EscapeSequence.getString(22);
                    strOption += EscapeSequence.getString(30);
                    break;
                case 7:
                    strOption += EscapeSequence.getString(23);
                    strOption += EscapeSequence.getString(31);
                    break;
                case 8:
                    strOption += EscapeSequence.getString(24);
                    strOption += EscapeSequence.getString(32);
                    break;
                default:
                    strOption += EscapeSequence.getString(17);
                    strOption += EscapeSequence.getString(25);
                    break;
            }

            posPrinter.printNormal(POSPrinterConst.PTR_S_RECEIPT, strOption + data);
        } catch (JposException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }


    public boolean printImage(String path, int width, int alignment, int brightness, int dither, int compress) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BM_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BM_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BM_RIGHT;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            byteBuffer.put((byte) brightness); // brightness
            byteBuffer.put((byte) compress); // compress
            byteBuffer.put((byte) dither); // dither

            posPrinter.printBitmap(byteBuffer.getInt(0), path, width, alignment);

        } catch (JposException e) {
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }


    public boolean printImage(Bitmap bitmap, int width, int alignment, int brightness, int dither, int compress) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BM_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BM_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BM_RIGHT;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
            byteBuffer.put((byte) brightness); // brightness
            byteBuffer.put((byte) compress); // compress
            byteBuffer.put((byte) dither); // dither

            posPrinter.printBitmap(byteBuffer.getInt(0), bitmap, width, alignment);

        } catch (JposException e) {
            e.printStackTrace();

            ret = false;
        }

        return ret;
    }

    public boolean printBarcode(String data, int symbology, int width, int height, int alignment, int hri) {
        boolean ret = true;

        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            if (alignment == ALIGNMENT_LEFT) {
                alignment = POSPrinterConst.PTR_BC_LEFT;
            } else if (alignment == ALIGNMENT_CENTER) {
                alignment = POSPrinterConst.PTR_BC_CENTER;
            } else {
                alignment = POSPrinterConst.PTR_BC_RIGHT;
            }

            posPrinter.printBarCode(POSPrinterConst.PTR_S_RECEIPT, data, symbology, height, width, alignment, hri);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    public boolean directIO(int command, byte[] data) {
        boolean ret = true;
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.directIO(command, null, data);
        } catch (JposException e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
    }

    public int getPrinterMaxWidth() {
        int width = 0;
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return width;
            }

            width = posPrinter.getRecLineWidth();
        } catch (JposException e) {
            e.printStackTrace();
        }

        return width;
    }

    public boolean beginTransactionPrint() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }
            posPrinter.transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_TRANSACTION);

        } catch (JposException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean endTransactionPrint() {
        try {
            if (!posPrinter.getDeviceEnabled()) {
                return false;
            }

            posPrinter.transactionPrint(POSPrinterConst.PTR_S_RECEIPT, POSPrinterConst.PTR_TP_NORMAL);
        } catch (JposException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    @Override
    public void dataOccurred(DataEvent dataEvent) {

    }

    @Override
    public void directIOOccurred(DirectIOEvent directIOEvent) {

    }

    @Override
    public void errorOccurred(ErrorEvent errorEvent) {

    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent outputCompleteEvent) {

    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {

    }
}