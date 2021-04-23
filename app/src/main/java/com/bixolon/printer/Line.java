package com.bixolon.printer;

public class Line {

        public String type;
        public String value;
        public int alignment;
        public int textsizeheight;
        public int textsizewidth;
        public int attribute;
        public int symbology;
        public int textposition;

        public Line(String type, String value, int alignment, int textsizeheight, int textsizewidth, int attribute, int symbology, int textposition) {

                this.type = type;
                this.value = value;
                this.alignment = alignment;
                this.textsizeheight = textsizeheight;
                this.textsizewidth = textsizewidth;
                this.attribute = attribute;
                this.symbology = symbology;
                this.textposition = textposition;
        }
}
