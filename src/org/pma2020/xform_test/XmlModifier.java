package org.pma2020.xform_test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* Taken from: https://www.mkyong.com/java/how-to-modify-xml-file-in-java-dom-parser/ */

/** Modifies an XML file for purposes of use with XFormTest
 *
 * Makes string text replacements for the following purposes:
 * - to correct erroneous modifications made by XLSX to XForm converters
 * - to handle currently unsupported features */
class XmlModifier {
    private String newFilePath;
//    private Document xmlDom;
    private String xmlString;

    /** Constructor
     *
     * @param filePath Path to xml file
     * @throws IOException if thrown by callee */
    XmlModifier(String filePath) throws IOException {
        newFilePath = filePath.substring(0, filePath.length() -4) + "-modified" + ".xml";
//        xmlDom = createXmlDom(filePath);
        xmlString = createXmlString(filePath);
    }

    /** Reads XML file and loads into string
     *
     * @param filePath Path to XML file
     *
     * @return New XML file string
     *
     * @throws IOException If thrown by callee */
    private String createXmlString(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    /** Writes to file
     *
     * @throws IOException if thrown by callee */
    void writeToFile() throws IOException {
        FileWriter file = new FileWriter(newFilePath);
        file.write(xmlString);
        file.flush();
        file.close();
    }

    /** Extracts function name from a given string.
     *
     * @param str The string containing function name; usually programming logic, containing func call.
     *
     * @return function name if present, else empty string */
    private String extractFunctionName(String str) {
        ArrayList<String> funcNameMatches = new ArrayList<>();

        Matcher funcNameMatcher = Pattern.compile("^[a-z]*\\(").matcher(str);
        while(funcNameMatcher.find())
            funcNameMatches.add(funcNameMatcher.group());

        if (funcNameMatches.size() < 1)
            return "";
        else {
            String match = funcNameMatches.get(0);
            return match.substring(0, match.length() - 1);
        }
    }

    /** Helper function to assist with function name matching
     *
     * Ideally, this would be replaced with a better regular expression.
     *
     * @param broadMatch Match to something that is almost like what we are looking for; a function call text
     *
     * @return The correct match text for function call text */
    private String handleFunctionPatterns(String broadMatch) {
        String funcName = extractFunctionName(broadMatch);

        if (funcName.equals(""))
            return broadMatch;
        else {
            StringBuilder match = new StringBuilder();
            String funcNamePlusParens = funcName + "(";
            match.append(funcNamePlusParens);
            int totOpenParens = 1;
            int totCloseParens = 0;

            for (int i = funcNamePlusParens.length(); i < broadMatch.length(); i++){
                char c = broadMatch.charAt(i);
                match.append(c);

                if (c == '(')
                    totOpenParens++;
                else if (c == ')')
                    totCloseParens++;
                if (totOpenParens == totCloseParens)
                    break;
            }

            return match.toString();
        }
    }

    /** Finds and replaces text in an XML string
     *
     * @param find text to find
     * @param replace text to replace with
     * @return true if any modifications were made, else false */
    boolean findReplace(String find, String replace) {
        // temp: https://www.regextester.com/97778
        // TODO
        // e.g. literal test1="true()" <- test that this and false() are being replaced correctly.
        Matcher matcher = Pattern.compile(find, Pattern.DOTALL).matcher(xmlString);
        ArrayList<String> matches = new ArrayList<>();
        ArrayList<String> broadMatches = new ArrayList<>();

        while(matcher.find())
            broadMatches.add(matcher.group());

        // corrections - properly match single function
        for (String broadMatch : broadMatches) {
            String match = handleFunctionPatterns(broadMatch);
            matches.add(match);
        }

        // TODO - swap these? does it even matter?
         ArrayList<String> matchesToRecurse = new ArrayList<>(matches);
//        ArrayList<String> matchesToRecurse = matches;

        // replacements
        // for (String match : matches) {
        for (int i = 0; i < matches.size(); i++) {
            String match = matches.get(i);
            if (replace.equals("*")) {  // implementation of xform-test glob syntax for replace
                String funcWrapperToRemove = find.replace(".*", "")
                        .replace("\\", "")
                        .replace(")", "");
                String contentsToKeep = match.substring(0, match.length() - 1)
                        .replace(funcWrapperToRemove, "");
                xmlString = xmlString.replace(match, contentsToKeep);

            } else {
                // check if it is a calculation override, e.g. of the form 'pulldata(): VALUE'
                String calculateAssertionSuffix = "()";
                String funcName = extractFunctionName(match);
                boolean isCalculateAssertion = match.equals(funcName + calculateAssertionSuffix);
                if (isCalculateAssertion)
                    //noinspection SuspiciousListRemoveInLoop
                    matchesToRecurse.remove(i);
                else {
                    // Examples: pulldata\(.*\)
                    if (replace.equals("'" + find + "'")) {
                        // TODO: Temp; testing what will actually not crash
                        xmlString = xmlString.replace(match, "test-pulldata");
                        // xmlString = xmlString.replace(match, "'" + match + "'");  // encapsulate in string

                        //noinspection SuspiciousListRemoveInLoop
                        matchesToRecurse.remove(i);
                    } else {
                        xmlString = xmlString.replace(match, replace);  // literal find replace
                    }
                }
            }
            if (matchesToRecurse.size() > 0)
                findReplace(find, replace);
        }

        //noinspection UnnecessaryLocalVariable
        boolean modificationsMade = broadMatches.size() > 0;
        return modificationsMade;
    }

    /** Getter: newFilePath
     *
     * @return File path of saved temporary file */
    String getnewFilePath() {
        return newFilePath;
    }
}
