package org.pma2020.xform_test;
// TODO's
// @0.1Release: Look over #to-do's.
// @0.1Release: Write tests: (1) check if assertions pass as expected, (2) check errors as expected
//   (3) repeat instances 2+.
// @0.1Release: Try importing the following from JavaRosa JAR (src and test): FormParseInit, ResourcePathHelper,
//   StubPropertyManager
// @Whenever: Look for #to-do for non-critical todos.
// @Whenever: Handle nested repeats.
// @Whenever: Better handling of commas and string literals.
// @Whenever: Handle an assertion like this; 2 string literals in a repeat: [252: 123-636&#x0a;, 777: 999-444&#x0a;]
// @Whenever: Validate things like this: "yes, relevant: 0"
// @Next release - delete the 'modified' xml immediately after being read into memory instead.

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.javarosa.core.model.DataType;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.ItemsetBinding;
import org.javarosa.core.model.Safe2014DagImpl;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.QuickTriggerable;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.Triggerable;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

// #to-do this might be useful for unit testing my own framework
//import org.javarosa.xform_test.FormParseInit;
//import org.junit.Before;
//import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

// #to-do: Add documentation: https://www.youtube.com/watch?v=heh4OeB9A-c&feature=youtu.be&t=33m57s
// #to-do: Make JavaDoc
/** XFormTest is a tool for creating and running tests to ensure the quality and stability of XForms.
 * Documentation: http://xform-test.pma2020.org/ */
@SuppressWarnings("unchecked")
public class XFormTest {
    private static final List<String> validAssertions = unmodifiableList(asList(
            // "constraint",  // #to-do: Boolean. Asserts constraint violation. Hasn't yet been implemented.
            // "error", // #to-do: String. Asserts that a value entered will error. Hasn't been implemented.
            "relevant",  // Boolean. Will the question appear?
            "value" // String. What value would you have entered on this node for this test case?
    ));
    private static final List<String> validDirectives = unmodifiableList(Collections.singletonList(
            "set-value" // String. Overrides evaluation of calculate fields. Sets value specified on node.
    ));
    private static final List<String> validAssertionsAndStaticDirectives;
    static {
        List<String> list = new ArrayList<>();
        list.addAll(validAssertions);
        list.addAll(validDirectives);
        validAssertionsAndStaticDirectives = list;
    }
    private static final Map<String, String> errorMessages;
    static {
        Map<String, String> errors = new HashMap<>();
        String excDetails = "\n\nErrored on: " +
                "\n - Node name: " + "%s" +
                "\n - Assertion text: " + "%s";
        errors.put("unableToEnterValueError", "Asserted enterable value '%s' was not able to be entered on node %s.");
        errors.put("invalidFieldExc", "In parsing assertions, an invalid field name was found. Valid field names " +
                "are: " + validAssertionsAndStaticDirectives + excDetails + "\n - Field: " + "%s");
        errors.put("valueAssertionExcNote", "Invalid assertion type: value assertion. Question/prompt '%s' " +
                "of type 'note' is read only. It is impossible to assert enterable value of assertion '%s'." +
                excDetails);
        errors.put("valueAssertionExcReadOnly", "Invalid assertion type: value assertion. Question/prompt " +
                "is read only. It is impossible to assert enterable value." + excDetails);
        errors.put("ValueAssertionError",
                "Asserted expected value '%s' did not match actual value of node '%s' on node %s.");
        errors.put("UnableToEvaluateError", "Could not evaluate calculate because relevant evaluated to false.");
        errors.put("invalidCalculateUsageError",
                "Node passed to assertCaculateEval() does not represent an instance of an XLSForm calculate.");
        errors.put("invalidCalculateAssertionError", "Calculate '%s' did not evaluate as expected." +
                "\nExpected: %s" +
                "\nGot: %s");        
        errors.put("XFormTestIllegalArgumentException", "Non-XML file passed as argument. " +
                "Please pass only XML files. \n\nErrored on the following file:\n");
        errors.put("commaColonExc", "The comma ',' character is currently reserved for syntax usage only, for the " +
                "purpose of delimiting multiple assertions. The colon ':' character is currently reserved also for " +
                "syntax usage only, for the purpose of delimiting assertion type from assertion value. " +
                "If the literal text of your assertions contain commas or colons, please remove them, as such usage" +
                "is currently unsupported. If you experience this issue and feel that support for literal usage of " +
                "these characters in your assertions would be helpful, please file an issue: " +
                "https://github.com/PMA-2020/xform-test/issues/new" + excDetails);
        errors.put("NodeNotFoundException", "Node '%s' was not found in form '%s'.");
        errors.put("MissingAssertionError", "Question '" + "%s" + "' was absent of any " +
                "assertions. XFormTest requires that at the very least, every answerable, required question includes " +
                "a value assertion for a linear scenario test to be considered valid. If you do not expect this " +
                "question to be relevant, and left off an assertion for that reason, please insert 'relevant: 0' " +
                "as the assertion.");
        errors.put("relevantFalseError", "\nThis is because the question relevant evaluated to false.");
        errors.put("choiceNotFoundError", " This choice option was not found in the list of options.");
        errors.put("inconsistentRelevantSyntaxError", "An inconsistent number of repeat instances were implicitly " +
                "asserted. Check each question's assertion syntax (e.g. [<assertions>, <assertions, ...]) and " +
                "make sure they all have the same number of commas.");
        errors.put("RelevantSyntaxException", "Relevant statement syntax on '%s' was invalid." +
                "\nExpected: true || 1 || false || 0" +
                "\nGot: %s");
        errors.put("RelevantAssertionError", "Relevant for '%s' did not evaluate as expected." +
                "\nExpected: %s" +
                "\nGot: %s");
        // TODO: translate below added new errors
        String evalOverrideDescriptions = "(1) the general-purpose 'set-value' directive, e.g. 'set-value: VALUE', or" +
                " (2) the function-specific evaluation overrides of the form 'funcName(): VALUE'";
        errors.put("invalidOverrideSyntax", "The field '%s' was declared, but is not a recognized " +
                "assertion or directive.\n" +
                "- List of valid assertions: " + validAssertions + "\n" +
                "- List of valid static directives: " + validDirectives + "\n" +
                "- Syntax for an evaluation override directive: functionName(): valueToOverrideWith");
        errors.put("conflictingDirectivesError", "A node was found where more than one type of value override " +
                "directives were used. Only one can be used per node, either " + evalOverrideDescriptions + ". " +
                "Please edit the node to only use one of these overrides, and try again.\n" +
                "- Name of offending node: %s");
        errors.put("OverrideDirectivesUsageException", "An evaluation override was used on a non-calculate node. " +
                "Neither " + evalOverrideDescriptions + " should be used except on a calculate.\n" +
                "- Name of offending node: %s");
        errorMessages = errors;
    }
    private static final List<String> validXformTestBindNames = unmodifiableList(asList(
        "xform-test",
        "xtest",
        "test"
    ));
    /* TODO: Actually, it looks like these can be custom. For example, in JHU Collect, we have a "logging"
        feature. If in settings, the 'logging' field is set to 'TRUE', the following will display in meta in XML:
        <meta>
            <instanceID/>
            <instanceName/>
            <logging/>
        </meta>
        Therefore, a good method to solve custom meta is to simply ignore every node who's parent is "meta". */
    private static final List<String> nonInteractiveInstanceNodes = unmodifiableList(asList(
        "start",
        "end",
        "deviceid",
        "phonenumber",
        "simserial",
        "meta",
        "instanceID",
        "instanceName",
        "logging"
    ));
    // Note: This is confusing as is. These are only partly regular expressions, and then more regexp logic
    // is tacked on later in multiple places. Should refactor.
    private static final Map<String, String> unsupportedFeatureFindReplaces;
    static {
        Map<String, String> mapping = new HashMap<>();
        // mapping.put("pulldata\\(.*\\)", "1");
        mapping.put("pulldata\\(.*\\)", "'pulldata\\(.*\\)'");  // Will wrap what is found in single quotes
        mapping.put("trim\\(.*\\)", "*");  // Will replace with *
        unsupportedFeatureFindReplaces = mapping;
    }
    private static final Map<String, String> converterIssueCorrections;
    static {
        Map<String, String> mapping = new HashMap<>();
        // e.g. literal test1="true()"
        mapping.put("true\\(\\)", "yes");
        mapping.put("false\\(\\)", "no");
        converterIssueCorrections = mapping;
    }
    private static final Map<Integer, String> formEntryControllerEntryStatusMapping;
    static {
        Map<Integer, String> mapping = new HashMap<>();
        mapping.put(0, "ANSWER_OK");
        mapping.put(1, "ANSWER_REQUIRED_BUT_EMPTY");
        mapping.put(2, "ANSWER_CONSTRAINT_VIOLATED");
        formEntryControllerEntryStatusMapping = Collections.unmodifiableMap(mapping);
    }
    private static final Map<Integer, String> formEntryControllerEventStatusMapping;
    static {
        Map<Integer, String> mapping = new HashMap<>();
        mapping.put(0, "EVENT_BEGINNING_OF_FORM");
        mapping.put(1, "EVENT_END_OF_FORM");
        mapping.put(2, "EVENT_PROMPT_NEW_REPEAT");
        mapping.put(4, "EVENT_QUESTION");
        mapping.put(8, "EVENT_GROUP");
        mapping.put(16, "EVENT_REPEAT");
        mapping.put(3, "EVENT_REPEAT_JUNCTURE");
        formEntryControllerEventStatusMapping = Collections.unmodifiableMap(mapping);
    }
    /** References: (1) XLSForm Types - http://xlsform.org/en/#question-types
     * ...(2) XForm-to-JavaRosa mappings - org.javarosa.xform.parse.TypeMappings.typeMappings */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final Map<Integer, String> javarosaValToDefMapping = new HashMap<>();
    static {
        for (DataType datatype : DataType.class.getEnumConstants()) {
            javarosaValToDefMapping.put(datatype.value, datatype.name());
        }
    }
    @SuppressWarnings("unused")
    private static final Map<String, List> javarosaToXlsformTypeMapping;  // #to-do utilize this and remove suppressor
    static {
        Map<String, List> mapping = new HashMap<>();
        // #to-do notes and calculates?
        // #to-do flesh this out, including the following types.
        //   range? - int or decimal depending on step?
        //   image? - binary? text (path string)?
        //   audio? - binary? text (path string)?
        //   video? - binary? text (path string)?
        //   hidden?
        //   xml-external?
        mapping.put("UNSUPPORTED", unmodifiableList(Collections
            .emptyList()));  // Refers2nonXLSForm,XForm-only typedefs
        // Declared 'decimal' in XLSForm but can be XForms can hold either long or non-long floats.
        mapping.put("LONG", unmodifiableList(Collections.singletonList("decimal")));
        mapping.put("CHOICE", unmodifiableList(Collections.singletonList("select_one")));  // TODO: Correct?
        mapping.put("MULTIPLE_ITEMS", unmodifiableList(Collections.singletonList("select_multiple")));  // TODO:Correct?
        mapping.put("BOOLEAN", unmodifiableList(Collections.singletonList("acknowledge")));  // correct?
        mapping.put("BINARY", unmodifiableList(Collections.singletonList("file")));  // correct?
        mapping.put("INTEGER", unmodifiableList(Collections.singletonList("integer")));
        mapping.put("DECIMAL", unmodifiableList(Collections.singletonList("decimal")));
        mapping.put("DATE", unmodifiableList(Collections.singletonList("date")));
        mapping.put("TIME", unmodifiableList(Collections.singletonList("time")));
        mapping.put("GEOPOINT", unmodifiableList(Collections.singletonList("geopoint")));
        mapping.put("BARCODE", unmodifiableList(Collections.singletonList("barcode")));
        mapping.put("GEOSHAPE", unmodifiableList(Collections.singletonList("geoshape")));
        mapping.put("GEOTRACE", unmodifiableList(Collections.singletonList("geotrace")));
        mapping.put("NULL", unmodifiableList(asList(
            "begin group",
            "end group",
            "begin repeat",
            "end repeat",
            "meta")));
        mapping.put("TEXT", unmodifiableList(asList(
            "text",
            "phonenumber",
            "simserial",
            "deviceid")));
        mapping.put("DATE_TIME", unmodifiableList(asList(
            "dateTime",
            "start",
            "end")));
        javarosaToXlsformTypeMapping = Collections.unmodifiableMap(mapping);
    }
    @SuppressWarnings("FieldCanBeLocal")
    private static boolean reportOnTestType = false;

// #to-do utilize this
//    public static final int CONTROL_UNTYPED         = ControlType.UNTYPED.value;
//    public static final int CONTROL_INPUT           = ControlType.INPUT.value;
//    public static final int CONTROL_SELECT_ONE      = ControlType.SELECT_ONE.value;
//    public static final int CONTROL_SELECT_MULTI    = ControlType.SELECT_MULTI.value;
//    public static final int CONTROL_TEXTAREA        = ControlType.TEXTAREA.value;
//    public static final int CONTROL_SECRET          = ControlType.SECRET.value;
//    public static final int CONTROL_RANGE           = ControlType.RANGE.value;
//    public static final int CONTROL_UPLOAD          = ControlType.UPLOAD.value;
//    public static final int CONTROL_SUBMIT          = ControlType.SUBMIT.value;
//    public static final int CONTROL_TRIGGER         = ControlType.TRIGGER.value;
//    public static final int CONTROL_IMAGE_CHOOSE    = ControlType.IMAGE_CHOOSE.value;
//    public static final int CONTROL_LABEL           = ControlType.LABEL.value;
//    public static final int CONTROL_AUDIO_CAPTURE   = ControlType.AUDIO_CAPTURE.value;
//    public static final int CONTROL_VIDEO_CAPTURE   = ControlType.VIDEO_CAPTURE.value;
//    public static final int CONTROL_OSM_CAPTURE     = ControlType.OSM_CAPTURE.value;
//    /** generic upload */
//    public static final int CONTROL_FILE_CAPTURE    = ControlType.FILE_CAPTURE.value;
//    public static final int CONTROL_RANK            = ControlType.RANK.value;

    private FormDef formDef;
    private FormEntryController formEntryController;
    private ArrayList<TreeElement> currentRepeatNodes = new ArrayList<>();
    private String currentEventState = "";
    private String thisRepeatInstanceAssertionStr = "";
    private int currentRepeatTotIterationsAsserted = 0;
    private int currentRepeatNum = 0;
    private Map warnings = new HashMap();
    private ArrayList<HashMap<String, Object>> testCases = new ArrayList<>();
    private HashMap<String, XPathConditional> calculateList;
    private ArrayList<String> testFieldNames;

    /** Runs XFormTest spec tests on valid XForm XML files.
     *
     * @param args Plain text paths to valid XForm XML files.
     *
     * @throws IOException if (a) file not found when reading, (b) unable to write temporary file */
    public static void main(String[] args) throws IOException {
        ArrayList<List> results = new ArrayList<>();
        ArrayList<String> filePaths = new ArrayList<>();

        if (args.length == 0)
            System.err.println("XFormTestError: Must pass one or more files as arguments.");

        // get list of files
        for (String filePath : args) {
            String parentDir = String.valueOf(Paths.get(filePath).getParent());
            String filenameOrGlobPattern = filePath.replace(parentDir + "/", "");
            FileFilter fileFilter = new WildcardFileFilter(filenameOrGlobPattern);
            File dir = new File(parentDir);
            File[] files = dir.listFiles(fileFilter);
            assert files != null;  // #to-do: Return custom error text about file not found or malformed glob pattern.
            for (File file : files)
                filePaths.add(file.getAbsolutePath());
        }

        try {
            // run tests
            for (String filePath : filePaths)
                validateFile(filePath);
            for (String file : filePaths) {
                XFormTest testCase = new XFormTest();
                List fileResults = testCase.runTestsOnFile(file);
                results = (ArrayList<List>) ListUtils.union(results, fileResults);
            }
            // print results
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String prettyJsonString = gson.toJson(results);
            System.out.println(prettyJsonString);

        } catch (XFormTestException err) {
            // Delete temp files
            for (String filePath : filePaths) {
                String modifiedFilePath = filePath.replace(".xml", "-modified.xml");
                File modifiedFile = new File(modifiedFilePath);
                //noinspection ResultOfMethodCallIgnored  // Returns 'true' if delete successful, else false.
                modifiedFile.delete();
            }
            // System.err.println(err.getClass().getSimpleName() + ": " + err.getMessage());
            System.err.println(err.getMessage());
        }
    }

    /** Runs all applicable tests on a single file.
     *
     * @param filePath Plain text path to valid XForm XML file.
     *
     * @return list of results for all test cases run on file.
     *
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws RelevantAssertionError if relevant did not evaluate as expected.
     * @throws ValueAssertionError if value expected does not match value that exists on node.
     * @throws IOException if (a) file not found when reading, (b) unable to write temporary file */
    private ArrayList<Map> runTestsOnFile(String filePath) throws XFormTestException, IOException {
        String correctedFilePath = fixAndSaveXForm(filePath);
        testFieldNames = testFieldNamesList(correctedFilePath);
        if (testFieldNames.size() < 1) {
            // TODO: Translate this error to French
            throw new XFormTestException("MisisngColumnsError: No test columns were found.\n" +
                "- Did you forget to put 'bind::' at the front of the column name?\n" +
                "- Is your column named exactly as one of the following?: " + validXformTestBindNames);
        }

        ArrayList<Map> testCaseResultsList = new ArrayList<>();
        List<String> testTypesToRun = unmodifiableList(Collections.singletonList("linearAssertionTest"));
        String fileName = FilenameUtils.removeExtension(new File(filePath).getName());
        for (String testType : testTypesToRun) {
            if (testType.equals("linearAssertionTest")) {
                for (String testCaseFieldName : testFieldNames) {
                    XFormTest testCase = new XFormTest();
                    FormEntryController formEntryController = testCase.setUpAndGetController(correctedFilePath);
                    ArrayList<TreeElement> instanceNodes = instanceNodes(formEntryController);
                    ArrayList<TreeElement> formElements = instanceNodesToFormElements(instanceNodes);
                    try {
                        Map results = testCase.linearAssertionTest(formElements, testCaseFieldName);
                        Map testCaseResults = testCaseResults(testType, results, testCaseFieldName, fileName);
                        testCaseResultsList.add(testCaseResults);
                    } catch (XFormTestException err) {
                        throw new XFormTestException("Test case '" + testCaseFieldName + "' in file '" + fileName +
                                "' failed.\n" + err.getClass().getSimpleName() + ": " + err.getMessage());
                    }
                }
            }
        }

        return testCaseResultsList;
    }

    /** Given a list of XForm form elements, find all the <bind/> attributes that are valid XFormTest fields.
     *
     * @param filePath Plain text paths to valid XForm XML file.
     *
     * @return a list of valid XFormTest fields*/
    private static ArrayList<String> testFieldNamesList(String filePath) {
        ArrayList<String> fieldNames = new ArrayList<>();
        XFormTest tempInstance = new XFormTest();

        // Silence stdout while doing this first setup.
        PrintStream originalStream = System.out;
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b) {}  // NO-OP
        });
        System.setOut(dummyStream);
        FormEntryController tempFormEntryController = tempInstance.setUpAndGetController(filePath);
        System.setOut(originalStream);

        ArrayList<TreeElement> tempInstanceNodes = instanceNodes(tempFormEntryController);
        ArrayList<TreeElement> tempFormElements = instanceNodesToFormElements(tempInstanceNodes);

        for (TreeElement ele : tempFormElements) {
            for (TreeElement attr : ele.getBindAttributes()) {
                String attrName = attr.getName();
                if (!fieldNames.contains(attrName)) {
                    for (String validName : validXformTestBindNames) {
                        if (attrName.startsWith(validName))
                            fieldNames.add(attrName);
                    }
                }
            }
        }

        return fieldNames;
    }

    /** Validates file and throws error early if it detects file is not valid.
     * @param fileName path to file to be validated.
     * @throws XFormTestIllegalArgumentException if file is not valid. */
    private static void validateFile(String fileName) throws XFormTestIllegalArgumentException {
       if (!fileName.endsWith(".xml"))
           throw new XFormTestIllegalArgumentException(errorMessages.get("XFormTestIllegalArgumentException") +
                   fileName);
    }

    /** Sets up a mock client. Necessary to handle cases where an XForm contains things that are specific to a given
     * XForm handling client, e.g. ODK Collect. */
    private static void setUpMockClient() {
        org.javarosa.core.services.PropertyManager.setPropertyManager(new StubPropertyManager());
    }

    /** Creates a new FormParseInit object, but silences all terminal output during the process.
     *
     * @param path Path object referencing a valid XForm XML file.
     *
     * @return an instantiated FormParseInit object. */
    private static FormParseInit squelchedFormParseInit(Path path) {
        PrintStream originalStream = System.out;
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b) {}  // NO-OP
        });
        System.setOut(dummyStream);
        FormParseInit fpi = new FormParseInit(path);  // The code to be squelched.
        System.setOut(originalStream);
        return fpi;
    }

    /** Fixes issues with source XML files that must be corrected before test can be run.
     *
     * Side effects:
     * - Writes a new file (callee)
     *
     * @param filePath Path to XML file
     *
     * @return Path to corrected file
     *
     * @throws IOException if (a) file not found when reading, (b) unable to write temporary file */
    private String fixAndSaveXForm(String filePath) throws IOException {
        XmlModifier xml = new XmlModifier(filePath);

        // 1. Handle unsupported features
        // TODO
        //  1. Calculate name appears as: ''pulldata\(.*\)''
        //  2. I think in test field, set-value 'pulldata: xxx' is causing error. Maybe replace it with something else?
        ArrayList<String> correctionsMade = handleUnsupportedFeatures(xml);
        if (correctionsMade.size() > 0)
            warnings.put("correctionsMade", correctionsMade);

        // TODO: Temp; refactor uncertain
        // 1.5 set field names
        xml.writeToFile();
        String filePath2 = xml.getnewFilePath();

        ArrayList<String> fieldNames = new ArrayList<>();
        XFormTest tempInstance = new XFormTest();

        // Silence stdout while doing this first setup.
        PrintStream originalStream = System.out;
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b) {}  // NO-OP
        });
        System.setOut(dummyStream);
        FormEntryController tempFormEntryController = tempInstance.setUpAndGetController(filePath2 );
        System.setOut(originalStream);

        ArrayList<TreeElement> tempInstanceNodes = instanceNodes(tempFormEntryController);
        ArrayList<TreeElement> tempFormElements = instanceNodesToFormElements(tempInstanceNodes);

        for (TreeElement ele : tempFormElements) {
            for (TreeElement attr : ele.getBindAttributes()) {
                String attrName = attr.getName();
                if (!fieldNames.contains(attrName)) {
                    for (String validName : validXformTestBindNames) {
                        if (attrName.startsWith(validName))
                            fieldNames.add(attrName);
                    }
                }
            }
        }

        testFieldNames = fieldNames;
        // TODO: end temp

        // 2. Fix form converter issues
        fixConverterIssues(xml);
        // 3. Save
        xml.writeToFile();

        return xml.getnewFilePath();
    }

    /** Fixes issues caused by XLSX to XForm converters
     *
     * @param xml XmlModifier instance ready for fixes */
    private void fixConverterIssues(XmlModifier xml) {
        // TODO #now: fix no --> false() and yes --> true()
        //  1. Add field name and "s
        //  2. add checkEscaping param
        // Example good and bad: required="true()" test1="\false()" test1="false()" test1="value: false()"
        Map<String, String> correctionsToMake = new HashMap<>();
        converterIssueCorrections.forEach((find, replace) -> {
            for (String testFieldName : testFieldNames) {
                String opener = testFieldName + "=\"";
                String closer = "\"";
                String newFind = opener + find + closer;
                String newReplace = opener + replace + closer;
                correctionsToMake.put(newFind, newReplace);
            }
        });
        correctionsToMake.forEach(xml::findReplace);
    }

    /** Creates new file with <code>unsupportedFeatureFindReplaces</code> removed.
     *
     * Side effects: Creates a new file.
     *
     * @param xml An XmlModifier class ready to run some modifications
     *
     * @return List of modifications made */
    private static ArrayList<String> handleUnsupportedFeatures(XmlModifier xml) {
        ArrayList<String> modificationsMade = new ArrayList<>();
        unsupportedFeatureFindReplaces.forEach((find, replace) -> {
            boolean modificationMade = xml.findReplace(find, replace);
            if (modificationMade)
                modificationsMade.add(find);
        });

        return modificationsMade;
    }

    /** Initializes a FormDef object, but silences all terminal output during the process.
     *
     * Side effects: Not a pure function. Modifies the following class props directly: formDef. */
    private void squelchedFormDefInit() {
        PrintStream originalStream = System.out;
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b) {}  // NO-OP
        });
        System.setOut(dummyStream);
        formDef.initialize(true, new InstanceInitializationFactory());  // The code to be squelched.
        System.setOut(originalStream);
    }

    /** Sets up some initial class properties.
     *
     * TODO 2019.04.26-jef: At time of this writing, this is called twice. It feels like these instance attributes
     * should only be set once.
     *
     * @param filePath The plain text path to a valid XForm XML file.
     *
     * @return formEntryController JavaRosa's controller class for doing form entry.
     *
     * Side effects:
     *   1. <code>formDef</code> mutation: Initializes.
     *   2. <code>calculateList</code> mutation: Initializes.
     *   3. <code>formEntryController</code> mutation: Initializes */
    private FormEntryController setUpAndGetController(String filePath) {
        setUpMockClient();
        FormParseInit fpi = squelchedFormParseInit(Paths.get(filePath));
        formDef = fpi.getFormDef();
        calculateList = calculateList(formDef);
        squelchedFormDefInit();
        FormEntryModel formEntryModel = new FormEntryModel(formDef);
        formEntryController = new FormEntryController(formEntryModel);

        return formEntryController;
    }

    /** Prints results of a test.
     * @param testType The type of test that was run.
     * @param results The test results.
     * @param testCaseFieldName The test case field name, as in what comes after <code>bind::</code> in an excel file,
     *                          e.g. <code>bind::test1</code>, or simply the node attribute name itself in an XML file,
     *                          e.g. <code>test1</code>.
     * @param fileName name of file being tested, without file extension.
     *
     * @return Map of results. */
    private Map testCaseResults(String testType, Map results, String testCaseFieldName, String fileName) {
        Map resultsData = new HashMap();

        resultsData.put("filename", fileName);
        resultsData.put("testCaseFieldName", testCaseFieldName);
        if (reportOnTestType)
            resultsData.put("testType", testType);

        resultsData.put("success", String.valueOf(true));
        resultsData.put("successMsg", "SUCCESS!\n\n" +
                "100% (n=" + results.get("numTestcases") +") of tests passed!\n\n" +
                "Test case summary:\n" +
                results.get("testCases"));

        resultsData.put("failures", String.valueOf(false));
        resultsData.put("failuresMsg", "");

        resultsData.put("errors", String.valueOf(false));
        resultsData.put("errorsMsg", "");

        StringBuilder warningMsg = new StringBuilder();
        Map warnings = (Map) results.get("warnings");
        if (!warnings.isEmpty()) {
            resultsData.put("warnings", String.valueOf(!results.get("warnings").equals("{}")));
            warningMsg.append("WARNINGS\n\n" +
                    "XformTest currently does not support some features of the XForm spec, as well as other " +
                    "features that are specific to clients such as ODK Collect, SurveyCTO, etc. " +
                    "The following is a list of attributes and unsupported features of those attributes " +
                    "that have been found and replaced with some other text in the form " +
                    "'unsupportedFeatureFound: replacedWithText'.\n\n");
//            Map unsupportedFoundReplaced = new HashMap();
            ArrayList<String> correctionsMade = (ArrayList<String>) warnings.get("correctionsMade");
//            correctionsMade.forEach((attr, featuresReplaced) -> {
//                List<String> findReplaces = new ArrayList();
//                unsupportedFoundReplaced.put(attr, findReplaces);
//                for (String feature : (List<String>) featuresReplaced) {
//                    //noinspection SuspiciousMethodCalls
//                    findReplaces.add(feature + ": " + unsupportedFeatureFindReplaces.get(attr).get(feature));
//                }
//            });
//            for (String attr : (Set<String>) unsupportedFoundReplaced.keySet()) {
//                warningMsg.append(attr);
//                warningMsg.append(":\n");
//                for (String findReplaceText : (List<String>) unsupportedFoundReplaced.get(attr)) {
//                    warningMsg.append("  ");
//                    warningMsg.append(findReplaceText);
//                    warningMsg.append("\n");
//                }
//            }
            for (String replaced : correctionsMade) {
                warningMsg.append("  ");
                warningMsg.append(replaced);
                warningMsg.append(": ");
                warningMsg.append(unsupportedFeatureFindReplaces.get(replaced));
                warningMsg.append("\n");
            }
        }
        resultsData.put("warningsMsg", warningMsg);

        return resultsData;
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String prettyJsonString = gson.toJson(resultsData);
//        System.out.println(prettyJsonString);
    }

    /** Takes a nested tree structure of TreeElement's recurses through it, eventually returning a flat list.
     * @param toAppend A list of one or more TreeElements to append to the flattened list.
     * @return a flattened list.
     */
    private static ArrayList<TreeElement> flattenedNodeList(ArrayList<TreeElement> toAppend) {
        ArrayList<TreeElement> list = new ArrayList<>();
        for (TreeElement node : toAppend) {
            list.add(node);
            if (node.hasChildren()) {
                ArrayList<TreeElement> children = exposedTreeElementKids(node);
                list.addAll(flattenedNodeList(children));
            }
        }
        return list;
    }

    /** Returns node children via nested private property TreeElement.children.children.
     * @param node The TreeElement containing child nodes.
     * @return child nodes. */
    private static ArrayList<TreeElement> exposedTreeElementKids(TreeElement node) {
        Object preLevel = getPrivateProperty(node, "children");
        return (ArrayList<TreeElement>) getPrivateProperty(preLevel, "children");
    }

    /** Gets the list of element nodes in the XForm <instance/> node from XFormTest.formEntryController.
     * @return The list of nodes.
     *
     * #to-do: Add to docs that 'meta' is not supported.
     * #to-do: Consider removing: start, end, deviceid, simserial, phonenumber */
    private static ArrayList<TreeElement> instanceNodes(FormEntryController formEntryController) {
        TreeElement rootElement = formEntryController.getModel().getForm().getMainInstance().getRoot();
        ArrayList<TreeElement> topLevelNodes = exposedTreeElementKids(rootElement);
        return flattenedNodeList(topLevelNodes);
    }

    /** Gets list of form elements, i.e. instance nodes sans any that aren't interactive in an actual form.
     * @param instanceNodes the list of element nodes in the XForm <instance/> node from XFormTest.formEntryController.
     * @return A list of form elements, i.e. the 'instance nodes', sans any that don't represent any interactive part of
     * the XForm instance that would actually be subject to testing, i.e.:
     *   <start/>
     *   <end/>
     *   <deviceid/>
     *   <simserial/>
     *   <phonenumber/>
     *   <meta>
     *     <instanceID/>
     *     <instanceName/>
     *   </meta> */
    private static ArrayList<TreeElement> instanceNodesToFormElements(ArrayList<TreeElement> instanceNodes) {
        ArrayList<TreeElement> formElements = new ArrayList<>();
        for (TreeElement node : instanceNodes) {
            boolean interactiveNode = true;
            for (String item : nonInteractiveInstanceNodes) {
                if (node.getName().equals(item))
                    interactiveNode = false;
            }
            if (interactiveNode)
                formElements.add(node);
        }
        return formElements;
    }

    /** Reformats an assertion string
     *
     * @param str assertion string taken from a node
     *
     * @return formatted string */
    private static String formatAssertionStr(String str) {
        String naToken = ".";
        String emptyStringToken1 = "\"\"";
        String emptyStringToken2 = "''";

        // re-format dates
        str = str.replace(" 00:00:00", "");

        // re-format n/a character and empty string
        if (str.equals(naToken))
            str = "";  // 2019-05-22, changed from: str = null;
        else if (str.equals(emptyStringToken1) || str.equals(emptyStringToken2))
            str = "";

        return str;
    }

    /** Extracts a assertion string from a given node.
     *
     * @param node node containing assertion attribute
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * @return Assertion string */
    private String extractAssertionStr(TreeElement node, String testFieldName) {
        String assertionStr;

        if (thisRepeatInstanceAssertionStr.equals("")) {
            assertionStr = node.getBindAttributeValue(null, testFieldName);
            if (assertionStr != null) {
                if (assertionStr.charAt(0) == '{')
                    assertionStr = assertionStr.substring(1);
                if (assertionStr.charAt(assertionStr.length() -1) == '}')
                    assertionStr = assertionStr.substring(0, assertionStr.length() - 1);
            }

        } else
            assertionStr = thisRepeatInstanceAssertionStr;

        return assertionStr;
    }

    /** Returns a map of all valid XFormTest-spec assertions on a given node.
     *
     * @param node An sub-node within XForm <instance/> node, e.g. "<myQuestionNode/>".
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * @return A map of all assertion fields and the values they are asserting, e.g. "{relevant: true, value: false}"
     *
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws OverrideDirectivesUsageException if override was used on a non-calculate node
     *
     * #to-do: HashMap<String, Assertion>: ConstraintAssertion, RelevantAssertion, ValueAssertion
     * #to-do: Throw error there is a value assertion in read_only question. */
    private Map nodeAssertions(TreeElement node, String testFieldName) throws
            AssertionSyntaxException, AssertionTypeException, MissingAssertionError, OverrideDirectivesUsageException {
        String nodeType = xlsformType(node);
        List<String> assertionList = new ArrayList<>();
        Map<String, Object> assertionMap = new HashMap<>();
        for (String field : validAssertionsAndStaticDirectives)
            assertionMap.put(field, "");
        boolean isReadOnly = !node.isEnabled();
        String missingAssertionError = String.format(errorMessages.get("MissingAssertionError"), node.getName());

        String assertionStr = extractAssertionStr(node, testFieldName);
        if (assertionStr == null) {
            if (!nodeType.equals("note") && !nodeType.equals("geopoint") && !isReadOnly && node.isRequired())
                throw new MissingAssertionError(missingAssertionError);
            return new HashMap<>();
        }
        assertionStr = formatAssertionStr(assertionStr);


        boolean falsyAssertion = assertionStr == null || assertionStr.equals("");
        boolean isRepeatMemberWithUnsplitAssertions = false;

        if (!falsyAssertion)
            isRepeatMemberWithUnsplitAssertions = assertionStr.charAt(0) == ('[');
        if (isRepeatMemberWithUnsplitAssertions) {
            // split
            List<String> instanceAssertionsList =
                unmodifiableList(Arrays.asList(assertionStr.split("\\s*,\\s*")));
            thisRepeatInstanceAssertionStr = instanceAssertionsList.get(currentRepeatNum -1)
                .replace("[", "")
                .replace("]", "");
            return nodeAssertions(node, testFieldName);
        }

        int numCommas = 0;
        int numColons = 0;
        if (!falsyAssertion) {
            numCommas = Math.toIntExact(assertionStr.chars().filter(num -> num == ',').count());
            numColons = Math.toIntExact(assertionStr.chars().filter(num -> num == ':').count());
        }

        // single assertions & edge cases
        // The "ConstantConditions" inspection is wrong? Seems like a bug in IntelliJ upon my inspection.
        // //spection ConstantConditions,StatementWithEmptyBody
        if (falsyAssertion
                && node.isRequired()
                && !(nodeType.equals("note") || nodeType.equals("geopoint") || isReadOnly )) {
            throw new MissingAssertionError(missingAssertionError);
        } else if (numCommas == 0 && numColons == 0) {  // single un-named value assertion
            if (!falsyAssertion) {
                if (nodeType.equals("note"))
                    throw new AssertionTypeException(
                        String.format(errorMessages.get("valueAssertionExcNote"), assertionStr));
                // TODO: What if read only, but they have a default value and also assert it?
                // date / dateTime default value to current date / dateTime
                else if (isReadOnly && !nodeType.equals("date") && !nodeType.equals("dateTime"))
                    throw new AssertionTypeException(String.format(errorMessages.get("valueAssertionExcReadOnly"),
                        node.getName(), assertionStr));
            }
            assertionMap.put("value", assertionStr);
        } else if (numCommas == 0 && numColons > 0) {  // single named assertion
            assertionList.add(assertionStr);

        // multiple assertions
        } else if (numCommas > 0 && numColons > 0) {
            if (numColons > numCommas + 1 || numColons < numCommas)
                throw new AssertionSyntaxException(errorMessages.get("commaColonExc"));

            // split & trim
            if (assertionStr.indexOf(',') != -1) {
                String[] unTrimmedAssertionList = assertionStr.split(",");
                for (String assertion : unTrimmedAssertionList) {
                    assertionList.add(assertion.trim());
                }
            }
        }

        assertionMap = assertionListToMap(assertionList, node, assertionMap);
        thisRepeatInstanceAssertionStr = "";

        return assertionMap;
    }

    /** Handles any edge cases in assertion text that would otherwise cause issues, by modifying said text.
     *
     * @param assertionList A collection of assertions which may or may not contain edge cases.
     * @param node An sub-node within XForm <instance/> node, e.g. "<myQuestionNode/>".
     * @param existingMap an assertion map if any to be supplied, else <code>null</code>.
     *
     * @return A collection of assertions.
     *
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     *
     * #to-do move any edge cases to a static map variable at top of class and loop through them here. */
    private Map<String, Object> assertionListToMap(List<String> assertionList, TreeElement node, Map<String, Object>
            existingMap) throws AssertionSyntaxException, OverrideDirectivesUsageException {
        Map<String, Object> assertionMap;
        if (existingMap == null)
            assertionMap = new HashMap<>();
        else
            assertionMap = existingMap;

        for (String assertion : assertionList) {
            boolean isCalculateAssertion = false;
            String[] keyAndVal = assertion.split(":");
            String key = keyAndVal[0].trim();
            String val = keyAndVal[1].trim();
            if (key.endsWith("\\")) {  // check to see if they escaped a colon, i.e. "\:"
                key = "value";
                val = assertion.replace("\\:", ":");
            }
            // My IDE asked for this: String used in lambda should be final or effectively final.
            String finalKeyToSatisfyOddJavaRule = key;
            boolean recognizedField = validAssertionsAndStaticDirectives.stream().anyMatch(str -> str.trim().
                    equals(finalKeyToSatisfyOddJavaRule));
            if (!recognizedField) {
                // logic to check calc assertion
                ArrayList<String> matches = new ArrayList<>();
                Matcher matcher = Pattern.compile("^[a-z]*\\(\\)").matcher(key);
                while(matcher.find())
                    matches.add(matcher.group());

                if (matches.size() > 0)
                    isCalculateAssertion = key  // if adheres exactly to syntax 'funcName()'
                            .replace(matches.get(0), "").equals("");
            }
            boolean validField = recognizedField || isCalculateAssertion;
            if (!validField)
                throw new AssertionSyntaxException(String.format(errorMessages.get("invalidFieldExc"),
                        node.getName(), assertion, key));
            assertionMap.put(key, val);
        }

        Map<String, Object> assertionMap2 = extractEvaluationOverrides(node, assertionMap);
        validateSetvalueDirective(node, assertionMap2);

        return assertionMapTextReplacements(assertionMap2);
    }

    /** Given map of assertions and directives, returns just a list of the evaluation overrides.
     *
     * @param assertions map of assertions and directives
     *
     * @return list of evaluation overrides, if any
     *
     * @throws AssertionSyntaxException if not valid syntax for evaluation override
     * @throws OverrideDirectivesUsageException if override was used on a non-calculate node */
    private Map<String, Object> extractEvaluationOverrides(TreeElement node, Map<String, Object> assertions)
            throws AssertionSyntaxException, OverrideDirectivesUsageException {
        List<String> overridesFound = new ArrayList<>();

        Map<String, Object> assertionsCopy = new HashMap<>();
        for (String key : assertions.keySet())
            assertionsCopy.put(key, assertions.get(key));

        Iterator it = assertionsCopy.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            String key = (String) pair.getKey();

            boolean isPossibleOverride = true;
            for (String field : validAssertionsAndStaticDirectives) {
                if (field.equals(key))
                    isPossibleOverride = false;
            }
            if (isPossibleOverride)
                overridesFound.add(key);

            it.remove(); // avoids a ConcurrentModificationException
        }

        validateEvaluationOverrides(node, overridesFound);

        Map<String, String> overrideKeyVals = new HashMap<>();
        for (String override : overridesFound) {
            overrideKeyVals.put(override.replace("()", ""), (String) assertions.get(override));
            assertions.remove(override);
        }
        assertions.put("evaluationOverrides", overrideKeyVals);

        return assertions;
    }

    /** Validates a list of directives on a given node to make sure that they are all of the appropriate
     * syntax for an evaluation override, i.e. <code>funcName()</code>.
     *
     * @param overrides A list of possible overrides
     *
     * @throws AssertionSyntaxException if not valid syntax for evaluation override
     * @throws OverrideDirectivesUsageException if override was used on a non-calculate node
     *
     * #to-do: validate the override against actual text in the calculate to make sure it's there */
    private void validateEvaluationOverrides(TreeElement node, List<String> overrides)
            throws AssertionSyntaxException, OverrideDirectivesUsageException {
        if (overrides.size() == 0)
            return;

        if (!xlsformType(node).equals("calculate"))
            throw new OverrideDirectivesUsageException(
                    String.format(errorMessages.get("OverrideDirectivesUsageException"), node.getName()));

        for (String field : overrides)
            if (!field.endsWith("()"))
                throw new AssertionSyntaxException(String.format(errorMessages.get("invalidOverrideSyntax"), field));
    }

    /** Validates edge cases where user has declared "setValue" directive alongside 1 or more evaluation override
     * directives.
     *
     * @param node An sub-node within XForm <instance/> node, e.g. "<myQuestionNode/>".
     * @param assertions A collection of assertions which may or may not contain edge cases.
     *
     * @throws AssertionSyntaxException if more than 1 unique evaluation override directive type used
     * @throws OverrideDirectivesUsageException if override was used on a non-calculate node */
    private void validateSetvalueDirective(TreeElement node, Map<String, Object> assertions)
            throws AssertionSyntaxException, OverrideDirectivesUsageException {
        int intendedUniqueDirectivesUsed = 0;

        if (assertions.containsKey("set-value") && !assertions.get("set-value").equals(""))
            intendedUniqueDirectivesUsed += 1;
        Map<String, Object> overrides = (Map<String, Object>) assertions.get("evaluationOverrides");
        intendedUniqueDirectivesUsed += overrides.size();

        if (intendedUniqueDirectivesUsed > 0 && !xlsformType(node).equals("calculate"))
            throw new OverrideDirectivesUsageException(
                    String.format(errorMessages.get("OverrideDirectivesUsageException"), node.getName()));
        else if (intendedUniqueDirectivesUsed > 1)
            throw new AssertionSyntaxException(
                    String.format(errorMessages.get("conflictingDirectivesError"), node.getName()));
    }

    /** Handles any edge cases in assertion text that would otherwise cause issues, by modifying said text.
     *
     * @param assertions A collection of assertions which may or may not contain edge cases.
     * @return A collection of assertions.
     *
     * #to-do move any edge cases to a static map variable at top of class and loop through them here. */
    private static Map<String, Object> assertionMapTextReplacements(Map<String, Object> assertions) {
        if (assertions.get("value").equals("true()"))
            assertions.put("value", "yes");
        return assertions;
    }

    /** Gets the probable XLSForm source type of a given XForm node, by inference.
     * @param node Interactive instance node in an XForm.
     * @return The type as would appear in an XLSForm, e.g. 'integer'.
     *
     * - Reference to XLSForm Types: http://xlsform.org/en/#question-types
     * - This method makes use of the formDef class's "DAG" property. Typically, the DAG classes are used for event
     * triggering. However in this case, we're using it because it has useful XForm node bind information already
     * pre-processed that otherwise does not appear in the other classes where the nodes have been processed.
     *
     * #to-do: determine select_one_external - looks like comes out as 'text'
     * #to-do: determine select_multiple_external - comes out as 'text'? */
    private String xlsformType(TreeElement node) {
        String xlsformType = "";
        String javarosaType = javarosaValToDefMapping.get(node.getDataType());
        boolean isCalc = calculateList.containsKey(node.getName());

        // TODO
//        QuestionDef q = findQuestionByRef(ref, f);
//        if (q == null
//            || (q.getControlType() != Constants.CONTROL_SELECT_ONE
//            && q.getControlType() != Constants.CONTROL_SELECT_MULTI
//            && q.getControlType() != Constants.CONTROL_RANK)) {
//            return "";
//        }


        if (isCalc)
            xlsformType = "calculate";
        else if (javarosaType.equals("NULL")) {
            if (currentEventState.equals("EVENT_GROUP"))
                xlsformType = "begin group";
            else if (currentEventState.equals("EVENT_REPEAT") ||
                currentEventState.equals("EVENT_PROMPT_NEW_REPEAT") ||
                currentEventState.equals("EVENT_REPEAT_JUNCTURE"))
                xlsformType = "begin repeat";
        // TODO: 2019-06-10. CHOICE = select_one? MULTIPLE_ITEMS = select_multiple? Is this done?
//        } else if (javarosaType.equals("MULTIPLE_ITEMS")) {  // #to-do Determine which is: select_one || select_multiple
//            return String.valueOf(javarosaToXlsformTypeMapping.get(javarosaType));
        } else {
            List<String> allPossibleXlsformTypes = javarosaToXlsformTypeMapping.get(javarosaType);

            if (allPossibleXlsformTypes.size() == 0)
                // no-op
            //noinspection ConstantConditions; Jetbrain's inspection assessment seems wrong to me here.
            if (allPossibleXlsformTypes.size() == 1)
            xlsformType = allPossibleXlsformTypes.get(0);

            ArrayList<String> possibleInteractiveXlsformTypes = new ArrayList();
            for (String item : allPossibleXlsformTypes) {
                boolean inIgnoreList = nonInteractiveInstanceNodes.stream().anyMatch(str -> str.equals(item));
                if (!inIgnoreList)
                    possibleInteractiveXlsformTypes.add(item);
            }
            if (possibleInteractiveXlsformTypes.size() == 1)
                xlsformType = possibleInteractiveXlsformTypes.get(0);

            if (xlsformType.equals("text") && !node.isEnabled())
                xlsformType = "note";  // in actuality could also be read_only text.
        }

        // Note 2019-06-10: No longer needed?
        //        // Convert to integer, decimal, or date if needed
        //        try {
        //            Long.parseLong(value);
        //            dataType = "long";
        //        } catch (Exception e) {
        //            try {
        //                Integer.parseInt(value);
        //                dataType = "int";
        //            } catch (Exception e2) {
        //                // no-op
        //            }
        //        }
        //

        return xlsformType;
    }

    /** Gets list of calculates extracting that information from within formDef.
     * @param formDef Form definition object.
     * @return List of calculates. */
    private HashMap<String, XPathConditional> calculateList(FormDef formDef) {
        HashMap<String, XPathConditional> calculateList = new HashMap<>();

        Safe2014DagImpl formDAG = (Safe2014DagImpl) getPrivateProperty(formDef, "dagImpl");
        ArrayList<QuickTriggerable> nodeListInDAG =
            (ArrayList<QuickTriggerable>) getPrivateProperty(formDAG, "triggerablesDAG");

        for (QuickTriggerable nodeRef : nodeListInDAG) {
            Triggerable node = nodeRef.t;
            ArrayList<TreeReferenceLevel> path =
                (ArrayList<TreeReferenceLevel>) getPrivateProperty(node.getTargets().get(0), "data");
            String nodeName = path.get(path.size()-1).getName();
            String dagNodeType = node.getClass().getSimpleName();

            if (dagNodeType.equals("Recalculate")) {
                boolean inIgnoreList = nonInteractiveInstanceNodes.stream().anyMatch(str -> str.equals(nodeName));
                if (!inIgnoreList)
                    calculateList.put(nodeName, (XPathConditional) node.getExpr());
            }
        }
        return calculateList;
    }

    /** Validates choice option against a choice selection quesetion (e.g. select_one, select_multiple, rank).
     * @param node An XForm instance node.
     * @param value The value expected to be enterable on a node.
     * @return true if valid, else false. */
    @SuppressWarnings("WeakerAccess")  // #note-1
    public boolean validChoiceOption(TreeElement node, String value) {
        TreeReference ref = node.getRef();
        QuestionDef q = FormDef.findQuestionByRef(ref, formDef);
        if (q == null)
            throw new NullPointerException(String.format(errorMessages.get("NodeNotFoundException"), node.getName(), formDef));
        List<SelectChoice> choices;
        ItemsetBinding itemset = q.getDynamicChoices();

        if (itemset != null) {
            if (itemset.getChoices() == null)
                formDef.populateDynamicChoices(itemset, ref);
            choices = itemset.getChoices();
        } else // static choices
            choices = q.getChoices();

        return choices.stream().anyMatch(choice -> choice.getValue().trim().equals(value));
    }

    /** Asserts that a node's value matches what is expected.
     * @param node An XForm instance node.
     * @param expectedValue The value expected.
     * @throws ValueAssertionError if value expected does not match value that exists on node. */
    @SuppressWarnings({"WeakerAccess"})  // #note-1 why are these warnings here? What if intend to be public?
    public static void assertValueMatches(TreeElement node, String expectedValue) throws ValueAssertionError {
        boolean match = false;
        Object actualValue = node.getValue();
        String actualValueStr = node.getValue().getDisplayText();

        if (actualValue.getClass() == DateData.class) {
            try {
                Date expectedDateValue = new SimpleDateFormat("yyyy-MM-dd").parse(expectedValue);
                Date actualDateValue = (Date) getPrivateProperty(actualValue, "d");
                match = expectedDateValue.equals(actualDateValue);
            } catch (ParseException e) {
                // no-op
            }
        } else {
            match = actualValueStr.equals(expectedValue);
        }

        if (!match)
            throw new ValueAssertionError(String.format(errorMessages.get("ValueAssertionError"),
                    expectedValue, actualValueStr, node.getName()));
    }

    /** Asserts value evaluates as expected on a calculate.
     *
     * @param node An XForm instance node representing an XLSForm calculate.
     * @param value The value expected to be evaluate correctly on node.
     *
     * @throws ValueAssertionError if value doesn't evaluate as expected on node, for whatever reason. */
    private void assertCalculateEval(TreeElement node, String value) throws XFormTestException {
        // TODO 7: solve calculates
        /* calculate issues
        - 1. all_selected_HH is not coming through either?
        - 2. this_country not eval to anything
          - For some reason this eval to 1 for max_num_hh but 0 for this_country
            - List<TreeReference> nodesetRefs = ec.expandReference(ref);
          - EvaluationContext breakpoint
            - ref.data.get(ref.data.size()-1).getName().equals("this_country") && !instance.name.equals("imei.csv")
            - ref.data.get(ref.data.size()-1).getName().equals("max_num_HH") && !instance.name.equals("imei.csv")
        */

        // TODO
        // convert to integer / decimal if needed
        //noinspection unused
        boolean isLong = false;
        //noinspection unused
        boolean isInt = false;
        try {
            Long.parseLong(value);
            //noinspection UnusedAssignment
            isLong = true;
        } catch (Exception e) {
            try {
                Integer.parseInt(value);
                //noinspection UnusedAssignment
                isInt = true;
            } catch (Exception e2) {
                // no-op
            }
        }
//        IntegerData a = new IntegerData(Integer.parseInt(value));
//        LongData b = new LongData(Long.parseLong(value));
//        StringData c = new StringData(value);

        if (!xlsformType(node).equals("calculate"))
            throw new RuntimeException(errorMessages.get("invalidCalculateUsageError"));

        if (!node.isRelevant())
            throw new ValueAssertionError(errorMessages.get("UnableToEvaluateError"));

        // to-do need not from calculate list but elsewhere?
        // to-do just pass it and get value?
        XPathConditional expression = calculateList.get(node.getName());
        EvaluationContext context = formEntryController.getModel().getForm().getEvaluationContext();
        DataInstance instance = formEntryController.getModel().getForm().getInstance();

        try {
            // not sure which of these is useful
            //////inspection UnusedAssignment
            Object result = expression.eval(instance, context);
            Object result2 = expression.evalRaw(instance, context);
//        Object result3 = expression.getExpr().eval(instance, context);
//        System.out.println(node.getName());
//        System.out.println("result1: " + result);
//        System.out.println("result2: " + result2);
//        System.out.println("result3: " + result3);
            // to-do: How to actually evaluate the calculate? Search javarosa/odk-collect source

            // to-do: this from form entry controller answer question?
            // TreeElement element = model.getTreeElement(index);
            
            //noinspection PointlessBooleanExpression,ConstantConditions
            if (!String.valueOf(result2).equals(value) && false)  // #to-do: Remove '&& false' when ready.
                throw new ValueAssertionError(String.format(errorMessages.get("invalidCalculateAssertionError"), 
                        node.getName(), value, String.valueOf(result)));
        } catch (XPathTypeMismatchException e) {
            // TODO: This happens when I make a value assertion on a repeat instance. If I do this, it does not
            // automatically know which instance, so I need to add indexed-repeat(.) maybe, or something like that,
            // perhaps at the beginning of this class somewhere.
        }
    }

    /** Asserts that a value can actually be entered on a node.
     * @param node An XForm instance node.
     * @param value The value expected to be enterable on a node.
     * @throws ValueAssertionError if value cannot be entered on a node, for whatever reason. */
    private void assertCanEnterValue(TreeElement node, String value) throws ValueAssertionError {
        String err = String.format(errorMessages.get("unableToEnterValueError"), value, node.getName());
        if (!node.isRelevant())
            throw new ValueAssertionError(err + "\n" + errorMessages.get("relevantFalseError"));
        
        if ((xlsformType(node).equals("select_one") || xlsformType(node).equals("select_multiple"))
            && !validChoiceOption(node, value))
            throw new ValueAssertionError(err + errorMessages.get("choiceNotFoundError"));

        int entryStatusCode;
        switch (xlsformType(node)) {
            case "integer":  // int
                entryStatusCode =
                    formEntryController.answerQuestion(new IntegerData(Integer.parseInt(value)), true);
                break;
            case "decimal":  // long
                entryStatusCode =
                    formEntryController.answerQuestion(new LongData(Long.parseLong(value)), true);
                break;
            case "date":
            case "dateTime":
                Date dateValue = null;
                try {
                    dateValue = new SimpleDateFormat("yyyy-MM-dd").parse(value);
                } catch (ParseException e) {
                    e.printStackTrace(); }
                // TODO 2019-06-10: read only date/dateTime should evaluate to current
                entryStatusCode = formEntryController.answerQuestion(new DateData(dateValue), true);
                break;
            case "select_one":
                Selection selectOneValue = new Selection(value);
                entryStatusCode = formEntryController.answerQuestion(new SelectOneData(selectOneValue), true);
                break;
            case "select_multiple":
                List<String> selections = asList(value.split(" "));
                List<Selection> selectMultipleValue = new ArrayList();
                selections.forEach((x) -> selectMultipleValue.add(new Selection(x)));
                entryStatusCode =
                        formEntryController.answerQuestion(new SelectMultiData(selectMultipleValue), true);
                break;
            default:
                // TODO: Other edge cases?
                try {
                    entryStatusCode = formEntryController.answerQuestion(new StringData(value), true);
                } catch (RuntimeException e) {
                    // TODO: Temp debugging: Non-question object at the form index; why?
                    // Currently at: group_review[1]
                    System.out.println();
                    entryStatusCode = 0;  // ANSWER_OK
                }
                break;
        }

        String entryStatus = formEntryControllerEntryStatusMapping.get(entryStatusCode);
        if (!entryStatus.equals("ANSWER_OK")) {
            if (entryStatus.equals("ANSWER_CONSTRAINT_VIOLATED")) {
                String constraintSyntaxTxt =
                         (String) getPrivateProperty(node.getConstraint().constraint, "xpath");
                err += " The constraint '" + constraintSyntaxTxt + "' was violated.";
            }
            throw new ValueAssertionError(err);
        }
        assertValueMatches(node, value);
    }

    /** Get a desired private/protected property of an object.
     * @param obj An object for which you want to get the private property.
     * @param prop The name of the property you want to get.
     * @return the property. */
    @SuppressWarnings("SameParameterValue")  // #to-do: not sure why this shows up. itll prolly go away when i use again
    private static Object getPrivateProperty(Object obj, String prop) {
        Object propValue = null;
        try {
            Field field = getPrivateField(obj.getClass(), prop);
            propValue = field.get(obj);
        } catch (SecurityException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return propValue;
    }

    /** Get a private/protected field of a class.
     * @param cls A class for which you want to get the private field.
     * @param prop The name of the field/property you want to get.
     * @return The now unprivate/unprotected field with which to access the property. */
    private static Field getPrivateField(Class cls, String prop) {
        Field field = null;
        try {
            field = cls.getDeclaredField(prop);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            field = getPrivateField(cls.getSuperclass(), prop);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return field;
    }

    /** Handles repeat groups
     *  - Initializes repeat groups
     *  - Handles navigation between repeat instances */
    private void handleRepeatNavigation(){
        if (currentRepeatNum == 0 || currentRepeatNum < currentRepeatTotIterationsAsserted) {
            // new repeat instance
            formEntryController.descendIntoNewRepeat();
            currentRepeatNum++;
        } else {
            // exit from repeat group
            currentRepeatTotIterationsAsserted = 0;
            currentRepeatNodes = new ArrayList<>();
            step();
        }
    }

    /** If first iteration of repeat is over and more repeats remain, recurse.
     *
     * @param testFieldName Name of test field <bind/> element (XLSForm column). */
    private void handleRepeatProcessingRecursively(String testFieldName) throws XFormTestException {
        boolean moreRepeatsRemain = currentRepeatNum <= currentRepeatTotIterationsAsserted;
        boolean repeatNodesInInstanceHaveBeenCheckedOnce = currentRepeatNum > 1;
        if (repeatNodesInInstanceHaveBeenCheckedOnce && moreRepeatsRemain)
            processNodes(currentRepeatNodes, testFieldName);
    }

    /** Handles repeat group nodes
     *  - Keeps track of node in repeat, for further iteration if necessary.
     *  - Validates repeat assertion syntax, e.g. "[<assertions>, <assertions, ...]".
     *
     * @param node Current node being executed.
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * @throws AssertionSyntaxException If inconsistent number of repeat instances implicitly asserted. */
    private void trackAndValidateRepeatNode(TreeElement node, String testFieldName) throws AssertionSyntaxException {
        // track - keep track of all nodes in repeat, for further iteration if necessary
        currentRepeatNodes.add(node);

        // validate - check for any inconsistencies in number of repeat group tests to be asserted
        String nodeAssertionStr = node.getBindAttributeValue(null, testFieldName);
        if (nodeAssertionStr.charAt(0) == ('[')) {
            int numIterationsAsserted = Math.toIntExact(nodeAssertionStr.chars().filter(num -> num == ',').count() + 1);
            if (currentRepeatTotIterationsAsserted == 0)
                currentRepeatTotIterationsAsserted = numIterationsAsserted;
            else if (currentRepeatTotIterationsAsserted != numIterationsAsserted)
                throw new AssertionSyntaxException(errorMessages.get("inconsistentRelevantSyntaxError"));
        }
    }

    /** Proceed forward one step in the form.
     * Side effects: Updates 'currentEventState' to reflect the state of the form reported by FormEntryController. */
    private void step() {
        int stepResult = formEntryController.stepToNextEvent();
        currentEventState = formEntryControllerEventStatusMapping.get(stepResult);
    }

    /** Runs assertion test on asserted values for a given node, and updates running testCases for report.
     *
     * @param node Instance XForm node containing XFormTest assertion bind.
     * @param value A value assertion on the XFormTest assertion bind. */
    private void handleValueAssertion(TreeElement node, String value) throws XFormTestException {
        HashMap<String, Object> nodeTestResults = new HashMap<>();
        FormEntryModel model = formEntryController.getModel();
        FormIndex index = model.getFormIndex();
        TreeElement activeInstanceNode  = formDef.getMainInstance().resolveReference(index.getReference());

        if (xlsformType(node).equals("calculate"))
            assertCalculateEval(node, value);
        else
            assertCanEnterValue(activeInstanceNode, value);

        nodeTestResults.put("name", node.getName());
        nodeTestResults.put("valueAssertion", value);
        testCases.add(nodeTestResults);
    }

    /** Runs assertion test on asserted relevant for a given node, and updates running testCases for report.
     * @param arbitraryNode Instance XForm node containing XFormTest assertion bind.
     * @param relevant A relevant assertion on the XFormTest assertion bind.
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws RelevantAssertionError if relevant did not evaluate as expected. */
    private void handleRelevantAssertion(TreeElement arbitraryNode, String relevant) throws AssertionSyntaxException,
            RelevantAssertionError {
        HashMap<String, Object> nodeTestResults = new HashMap<>();
//        FormEntryModel model = formEntryController.getModel();
//        FormIndex index = model.getFormIndex();
//        TreeElement activeInstanceNode = formDef.getMainInstance().resolveReference(index.getReference());
        // TODO: fix: Won't be active instance node if not relevant
        // TODO: why would arbitraryNode (uninstantiated) be == to instantiated node?
        TreeElement node = formDef.getMainInstance().resolveReference(arbitraryNode.getRef());

        boolean relevantSyntaxValid = (relevant.equals("true") || relevant.equals("1") ||
            relevant.equals("false") || relevant.equals("0"));
        if (!relevantSyntaxValid)
            throw new AssertionSyntaxException(String.format(errorMessages.get("RelevantSyntaxException"),
                    node.getName(), relevant));
        boolean assertedRelevancy = (relevant.equals("true") || relevant.equals("1"));
        if (node.isRelevant() != assertedRelevancy)
            throw new RelevantAssertionError(String.format(errorMessages.get("RelevantAssertionError"),
                    node.getName(), String.valueOf(assertedRelevancy), String.valueOf(node.isRelevant())));

        nodeTestResults.put("relevant", node.getName());
        nodeTestResults.put("relevantAssertion", String.valueOf(node.isRelevant()));
        testCases.add(nodeTestResults);
    }

    /** Tests assertions on a given node.
     *
     * @param node The TreeElement instance node which contain XFormTest bind information.
     * @param testFieldName Name of test field <bind/> element (XLSForm column). */
    private void evaluateAssertions(TreeElement node, String testFieldName) throws XFormTestException {
        Map<String, Object> assertions = nodeAssertions(node, testFieldName);

        if (!assertions.get("value").equals(""))
            handleValueAssertion(node, (String) assertions.get("value"));
        if (!assertions.get("relevant").equals(""))
            handleRelevantAssertion(node, (String) assertions.get("relevant"));
    }

    /** Iterate through nodes and run tests on them.
     *
     * @param arbitraryNodeset Nodes to iterate through. These could for example be the set of all instance nodes in an
     * XForm, or it might just be the set of all nodes in a given repeat group. These nodes may or may not be tied to
     * an active instance in a running form. In actuality, they probably are not.
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * @return True if finished as expected, else false. */
    private boolean processNodes(ArrayList<TreeElement> arbitraryNodeset, String testFieldName)
            throws XFormTestException {
        HashMap<String, Integer> nodeIndexLookup = new HashMap<>();
        for (int i=0; i<arbitraryNodeset.size(); i++)
            nodeIndexLookup.put(arbitraryNodeset.get(i).getName(), i);

        ArrayList<TreeElement> nodes = processNodesOverrideDirectives(arbitraryNodeset, testFieldName);
        //noinspection UnnecessaryLocalVariable
        boolean finishedAsExpected = processNodesAssertions(nodes, nodeIndexLookup, testFieldName);

        return finishedAsExpected;
    }

    /** Process all evaluation override directives on nodes, i.e. (1) the general-purpose 'set-value' directive, e.g.
     * 'set-value: VALUE', and (2) the function-specific evaluation overrides of the form 'funcName(): VALUE'.
     *
     * @param nodes Nodes to iterate through. These could for example be the set of all instance nodes in an
     * XForm, or it might just be the set of all nodes in a given repeat group. These nodes may or may not be tied to
     * an active instance in a running form. In actuality, they probably are not.
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * @return New list of nodes that have had their calculate elements modified as instructed by directives */
    private ArrayList<TreeElement> processNodesOverrideDirectives(ArrayList<TreeElement> nodes, String testFieldName)
            throws XFormTestException {
        ArrayList<TreeElement> processedNodes = new ArrayList<>(nodes);

        for (TreeElement node : processedNodes) {
            Map<String, Object> assertions = nodeAssertions(node, testFieldName);
            Map<String, String> overrides = (Map<String, String>) assertions.get("evaluationOverrides");

            if (overrides != null && overrides.size() > 0)
                handleEvaluationOverrideDirectives(node, (Map<String, String>) assertions.get("evaluationOverrides"));
            else if (!assertions.get("set-value").equals(""))
                handleSetValueDirective(node, (String) assertions.get("set-value"));
        }

        return processedNodes;
    }

    /** Process function-specific evaluation overrides of the form 'funcName(): VALUE'. For each override specified,
     * this modifies the raw calculate text, replacing an instance of that function call with static value specified.
     *
     * @param node Instance XForm node containing XFormTest assertion bind.
     * @param overrides a map of function names to be searched for and static values to be put in as replacements */
    private void handleEvaluationOverrideDirectives(TreeElement node, Map<String, String> overrides) {
        // assertCalculateEval(node, "to-replace");  // likely dont need to evaluate
        // TODO
        System.out.println(node);
        System.out.println(overrides);
    }

    /** Replaces any existing value of a node corrollary to XLSForm calculate to the new value passed.
     *
     * @param node Instance XForm node containing XFormTest assertion bind.
     * @param value New value to set on the node. */
    private void handleSetValueDirective(TreeElement node, String value) {
        // assertCalculateEval(node, "to-replace");  // likely dont need to evaluate
        // TODO
        System.out.println(node);
        System.out.println(value);
    }

    /** Process all test assertions on a nodeset.
     *
     * @param nodes Nodes to iterate through. These could for example be the set of all instance nodes in an
     * XForm, or it might just be the set of all nodes in a given repeat group. These nodes may or may not be tied to
     * an active instance in a running form. In actuality, they probably are not.
     * @param nodeIndexLookup A map of the node name to the index it appears in the form instance.
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * @return boolean, true if finished as expected, else false */
    private boolean processNodesAssertions(ArrayList<TreeElement> nodes, Map<String, Integer> nodeIndexLookup, String
            testFieldName) throws XFormTestException {
        for (TreeElement node : nodes) {
            if (currentRepeatNum == 1)  // Only needed once per repeat group.
                trackAndValidateRepeatNode(node, testFieldName);

            evaluateAssertions(node, testFieldName);

            if (!formEntryController.getModel().getFormIndex().isEndOfFormIndex()) {
                TreeElement activeInstanceNode = formDef.getMainInstance().resolveReference(
                        formEntryController.getModel().getFormIndex().getReference());
                int activeInstanceIndex = nodeIndexLookup.get(activeInstanceNode.getName());
                int arbitraryNodesetIndex = nodeIndexLookup.get(node.getName());
                if (activeInstanceIndex == arbitraryNodesetIndex)  // this may need work
                    step();
            }

            if (currentEventState.equals("EVENT_PROMPT_NEW_REPEAT")) {
                handleRepeatNavigation();
                handleRepeatProcessingRecursively(testFieldName);
            }
        }
        //noinspection UnnecessaryLocalVariable
        boolean finishedAsExpected = currentEventState.equals("EVENT_END_OF_FORM");
        return finishedAsExpected;
    }

    /** Runs a XFormTest 'linear assertion test'.
     *
     * # Background
     * XFormTest introduces a specific kind of test called the ‘linear scenario test’. A linear scenario test defined
     * as a set of unit tests or test assertions that are specifically designed to be executed in a linear sequence, one
     * after the other. The reason it can be important to test a set of questions answers or assertions linearly is due
     * to interdependencies. In forms of any non-trivial complexity, there exist many questions which are dependent on
     * other questions. Therefore, it is important useful test whole scenarios, rather than individual questions in
     * isolation.
     *
     * It is also possible that a singule survey might be very different depending on different scenarios. Imagine, for
     * example, that you are designing a family planning survey. The main target audience for the survey is women of
     * reproductive age. However, it could be that the survey workflow and the specific questions that are asked differ
     * greatly depending on various factors, such as (1) whether or not the respondent has used a particular method of
     * family planning, or (2) whether or not the respondent has ever had a live birth. Or, there may be alternative
     * questions asked even if the respondent is not within the target audience, that is, in this case, (3) not being a
     * woman of preproductive age. The examples (1), (2), and (3) describe very different scenarios where the survey
     * workflow and the types of questions asked might be very different. Therefore if testing such a form, it might be
     * prudent to test these 3 different scenarios individually.
     *
     * # Implementation
     * This implementation uses formEntryController to navigate through the form, but the primary iterator is a tree
     * of instance nodes. It is useful for clarity to note that each time the formEntryController proceeds to its next
     * step via 'stepToNextEvent', that is very often not the same thing as moving to the next instance node. This can
     * be because some instance nodes do not appear as events (e.g. 'calculate'), or because a given instance node's
     * relevant statement did not evaluate to 'true'.
     *
     * @param formElements Nodes to iterate through: all interactive instance nodes in based on but not in instance of
     *                     formEntryController.
     * @param testFieldName Name of test field <bind/> element (XLSForm column).
     *
     * #to-do: Need to try this only if detecting that it is a repeat group first.
     * #to-do: error out on currently unsupported assertions
     * #to-do: allow for assertions on currently unsupported XLSForm types: calculate
     *   May need to backtrack and check every node that hasn't occurred and check it? formEntryController.getModel();
     * #to-do: constraint assertions
     *   boolean constraintEval = form.evaluateConstraint(TreeReference ref, IAnswerData data);
     *   TreeElement .setValue() && .constraint || .getConstraint()?
     * #to-do: relevant assertions
     *   boolean relevantEval = state.isIndexRelevant(); */
    private Map linearAssertionTest(ArrayList<TreeElement> formElements, String testFieldName)
            throws XFormTestException {
        Map results = new HashMap<>();

        step();
        boolean success = processNodes(formElements, testFieldName);

        results.put("exitCode", success ? "0" : "1");
        results.put("testCases", String.valueOf(testCases));
        results.put("numTestcases", String.valueOf(testCases.size()));
        results.put("warnings", warnings);

        return results;
    }
}
