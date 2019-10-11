package com.gianlu.pretendyourexyzzy.api.models;

import android.content.Context;

import androidx.annotation.NonNull;

import com.gianlu.commonutils.adapters.Filterable;
import com.gianlu.pretendyourexyzzy.R;

import java.text.ParseException;
import java.util.Comparator;

public class Name implements Filterable<String> {
    private final String name;
    private final String noSigil;
    private final Sigil sigil;

    public Name(@NonNull String name) {
        this.name = name;
        this.noSigil = Sigil.removeSigil(name);
        this.sigil = Sigil.fromName(name);
    }

    @NonNull
    public String withSigil() {
        return name;
    }

    @NonNull
    public Sigil sigil() {
        return sigil;
    }

    @NonNull
    public String noSigil() {
        return noSigil;
    }

    @Override
    public String getFilterable() {
        return name;
    }

    public enum Sigil {
        ADMIN("@"), ID_CODE("+"), NORMAL_USER("");
        private final String val;

        Sigil(String val) {
            this.val = val;
        }

        @NonNull
        public static Sigil parse(@NonNull String val) throws ParseException {
            for (Sigil sigil : values())
                if (sigil.val.equals(val))
                    return sigil;

            throw new ParseException(val, 0);
        }

        @NonNull
        public static String removeSigil(@NonNull String name) {
            char first = name.charAt(0);
            if (first == '@' || first == '+') return name.substring(1);
            else return name;
        }

        @NonNull
        public static Sigil fromName(@NonNull String name) {
            char first = name.charAt(0);
            switch (first) {
                case '@':
                    return ADMIN;
                case '+':
                    return ID_CODE;
                default:
                    return NORMAL_USER;
            }
        }

        @NonNull
        public String symbol() {
            if (this == NORMAL_USER) return "<i>none</i>";
            else return val;
        }

        @NonNull
        public String getFormal(Context context) {
            switch (this) {
                case ADMIN:
                    return context.getString(R.string.admin) + " (" + val + ")";
                case ID_CODE:
                    return context.getString(R.string.idCodeLabel) + " (" + val + ")";
                default:
                case NORMAL_USER:
                    return context.getString(R.string.sigilNone);
            }
        }
    }

    public static class AzComparator implements Comparator<Name> {

        @Override
        public int compare(Name o1, Name o2) {
            return o1.noSigil.compareTo(o2.noSigil);
        }
    }

    public static class ZaComparator implements Comparator<Name> {

        @Override
        public int compare(Name o1, Name o2) {
            return o2.noSigil.compareTo(o1.noSigil);
        }
    }
}
