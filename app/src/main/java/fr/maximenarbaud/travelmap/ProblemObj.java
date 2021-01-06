package fr.maximenarbaud.travelmap;


import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProblemObj implements Comparable<ProblemObj> {
    private final String problemType;
    private final String problemText;
    private final Timestamp timestamp;

    public ProblemObj(String problemType, String problemText, Timestamp timestamp) {
        this.problemType = problemType;
        this.problemText = problemText;
        this.timestamp = timestamp;
    }

    public String getProblemType() { return problemType; }
    public String getProblemText() { return problemText; }
    public Timestamp getTimestamp() { return timestamp; }

    @Nullable
    public String getFormatTimestamp() {
        if (this.timestamp != null) {
            String dayNumber = new SimpleDateFormat("d", Locale.FRANCE).format(this.timestamp.toDate());
            String monthText = new SimpleDateFormat("MMMM", Locale.FRANCE).format(this.timestamp.toDate());
            monthText = firstToUpper(monthText);

            String yearNumber = new SimpleDateFormat("yyyy", Locale.FRANCE).format(this.timestamp.toDate());
            String time = new SimpleDateFormat("HH:mm", Locale.FRANCE).format(this.timestamp.toDate());

            return dayNumber + " " + monthText + " " + yearNumber + " à " + time;
        }

        return null;
    }

    private static String firstToUpper(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    public int compareTo(ProblemObj o) {
        return this.getTimestamp().compareTo(o.getTimestamp());
    }
}
