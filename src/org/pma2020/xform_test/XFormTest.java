/*
 * Copyright Joe Flack
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
// @Whenever: Remove '-modified' xml.

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
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.InstanceInitializationFactory;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.instance.TreeReferenceLevel;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xpath.XPathConditional;
import org.javarosa.xpath.XPathTypeMismatchException;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// #to-do this might be useful for unit testing my own framework
//import org.javarosa.xform_test.FormParseInit;
//import org.junit.Before;
//import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

// #to-do: Add documentation: https://www.youtube.com/watch?v=heh4OeB9A-c&feature=youtu.be&t=33m57s
// #to-do: Make JavaDoc
/** XForm-test is a tool for creating and running tests to ensure the quality and stability of XForms.
 * Documentation: http://xform-test.pma2020.org/ */
@SuppressWarnings("unchecked")
public class XFormTest {
    private static final List<String> validFields = unmodifiableList(asList(
        "constraint",
        "relevant",
        "value"
    ));
    private static final List<String> nonInteractiveInstanceNodes = unmodifiableList(asList(
        "start",
        "end",
        "deviceid",
        "phonenumber",
        "simserial",
        "meta",
        "instanceID",
        "instanceName"
    ));
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
    private static final HashMap<Integer, String> javarosaValToDefMapping = new HashMap<>();
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
    @SuppressWarnings("FieldCanBeLocal")
    private ArrayList<TreeElement> instanceNodes = new ArrayList<>();
    @SuppressWarnings("FieldCanBeLocal")
    private ArrayList<TreeElement> formElements = new ArrayList<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private ArrayList<String> warnings = new ArrayList<>();  // remove comment when warnings is used
    private ArrayList<HashMap<String, String>> testCases = new ArrayList<>();
    private HashMap<String, String> results = new HashMap<>();
    private HashMap<String, XPathConditional> calculateList;


    /** @param args XFormTest currently supports only one argument, the plain text path to a valid XForm XML file.
     * @throws IllegalArgumentException if more than one argument provided.
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws RelevantAssertionError if relevant did not evaluate as expected. */
    public static void main(String[] args) throws IllegalArgumentException, AssertionSyntaxException,
            AssertionTypeException, MissingAssertionError, RelevantAssertionError {
//        TODO: Handle no args
        if (args.length > 1) {
//            throw new IllegalArgumentException("Too many arguments supplied. Only one argument, the path to an XFORM" +
//                    "XML file, is currently supported.");
            System.out.println();
        }
        String filePathStr = args[0];
        XFormTest xFormTest = new XFormTest();
        System.out.println("\nRunning XForm Test");
        System.out.println("http://xform-test.pma2020.org");
        System.out.println("1. Loading form");
        xFormTest.setUp(filePathStr);
        xFormTest.linearAssertionTest();
    }

    /** Sets up a mock client. Necessary to handle cases where an XForm contains things that are specific to a given
     * XForm handling client, e.g. ODK Collect. */
    private static void setUpMockClient() {
        org.javarosa.core.services.PropertyManager.setPropertyManager(new StubPropertyManager());
    }

    /** Creates a new FormParseInit object, but silences all terminal output during the process.
     * @param path Path object referencing a valid XForm XML file.
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

    /** Removes pulldata(), an ODK Collect client-specific function not yet supported by XFormTest.
     * @param filePath The plain text path to a valid XForm XML file.
     * @return plain text path to a new XForm XML file that no longer contains 'pulldata()'.
     *
     * Side effects: Creates a new file.
     *
     * #to-do: Make a map of all invalid {attr: feature}s to be removed. */
    private static String removePullData(String filePath) {
        XmlModifier xml = new XmlModifier(filePath);
        xml.modifyNodeAttributesByFindReplace("calculate", "pulldata", "1");
        xml.writeToFile();
        return xml.getnewFilePath();
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
     * @param xmlFilePathStr The plain text path to a valid XForm XML file.
     *
     * Side effects: Not a pure function. Modifies the following class props directly: formEntryController. */
    private void setUp(String xmlFilePathStr) {
        setUpMockClient();
        String newXmlFilePath = removePullData(xmlFilePathStr);
        Path xmlFilePath = Paths.get(newXmlFilePath);
        FormParseInit fpi = squelchedFormParseInit(xmlFilePath);
        formDef = fpi.getFormDef();
        squelchedFormDefInit();
        FormEntryModel formEntryModel = new FormEntryModel(formDef);
        formEntryController = new FormEntryController(formEntryModel);
        calculateList = calculateList(formDef);
    }

    /** Prints results of a test.
     * @param testType The type of test that was run.
     * @param results The test results.
     *
     * Side effects: Prints to the terminal. */
    private void printResults(String testType, HashMap<String, String> results) {
        results.put("testCases", String.valueOf(testCases));
        results.put("warnings", warnings.isEmpty() ? "" : String.valueOf(warnings));
        String tot = String.valueOf(testCases.size());
        System.out.println("\nXForm Test '" + testType + "' result: 100% (n=" + tot + ") of tests passed!");
        System.out.println("Test case summary: ");
        System.out.println(results.get("testCases"));
        if (!results.get("warnings").equals("")) {
            System.out.println("\nWarnings: ");
            System.out.println(results.get("warnings"));
        }
    }

    /** Exits the program.
     * @param exitCode An exit code, 0 if success, 1 if failure.
     * @throws RuntimeException if a failing exit code was passed. */
    private static void exit(int exitCode) throws RuntimeException {
        if (exitCode == 0) {
            System.out.println("\nSuccess!");
        } else if (exitCode == 1) {
            throw new RuntimeException("\nFailure!");
        }
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

    /** Returns a map of all valid XFormTest-spec assertions on a given node.
     * @param node An sub-node within XForm <instance/> node, e.g. "<myQuestionNode/>".
     * @return A map of all assertion fields and the values they are asserting, e.g. "{relevant: true, value: false}"
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     *
     * #to-do: HashMap<String, Assertion>: ConstraintAssertion, RelevantAssertion, ValueAssertion
     * #to-do: Throw error there is a value assertion in read_only question. */
    private HashMap<String, String> nodeAssertions(TreeElement node) throws AssertionSyntaxException,
            AssertionTypeException, MissingAssertionError {
        String missingAssertionError = "Missing assertion error: Question '" + node.getName() + "' was absent of any " +
                "assertions. XFormTest requires that at the very least, every answerable, required question includes " +
                "a value assertion for a linear scenario test to be considered valid. If you do not expect this " +
                "question to be relevant, and left off an assertion for that reason, please insert 'relevant: 0' " +
                "as the assertion.";
        boolean isReadOnly = !node.isEnabled();
        HashMap<String, String> assertions = new HashMap<>();
        for (String field : validFields)
            assertions.put(field, "");
        List<String> assertionList = new ArrayList<>();

        String assertionsStr;
        if (thisRepeatInstanceAssertionStr.equals("")) {
            assertionsStr = node.getBindAttributeValue(null, "xtest-linearAssert");
            if (assertionsStr == null) {
                if (!xlsformType(node).equals("note") && !isReadOnly && node.isRequired())
                    throw new MissingAssertionError(missingAssertionError);
                return assertions;
            }
            if (assertionsStr.charAt(0) == '{')
                assertionsStr = assertionsStr.substring(1);
            if (assertionsStr.charAt(assertionsStr.length() -1) == '}')
                assertionsStr = assertionsStr.substring(0, assertionsStr.length() - 1);
        } else
            assertionsStr = thisRepeatInstanceAssertionStr;

        // re-format dates
        assertionsStr = assertionsStr.replace(" 00:00:00", "");
        // re-format n/a character
        String na_char = ".";
        if (assertionsStr.equals(na_char))
            assertionsStr = "";

        boolean isRepeatMemberWithUnsplitAssertions = false;
        if (!assertionsStr.equals(""))
            isRepeatMemberWithUnsplitAssertions = assertionsStr.charAt(0) == ('[');
        if (isRepeatMemberWithUnsplitAssertions) {
            // split
            List<String> instanceAssertionsList =
                unmodifiableList(Arrays.asList(assertionsStr.split("\\s*,\\s*")));
            thisRepeatInstanceAssertionStr = instanceAssertionsList.get(currentRepeatNum -1)
                .replace("[", "")
                .replace("]", "");
            return nodeAssertions(node);
        }

        int numCommas = Math.toIntExact(assertionsStr.chars().filter(num -> num == ',').count());
        int numColons = Math.toIntExact(assertionsStr.chars().filter(num -> num == ':').count());

        String excDetails = "\n\nErrored on: " +
            "\n - Node name: " + node.getName() +
            "\n - Assertion text: " + assertionsStr;
        String commaColonExc = "The comma ',' character is currently reserved for syntax usage only, for the " +
            "purpose of delimiting multiple assertions. The colon ':' character is currently reserved also for syntax" +
            "usage only, for the purpose of delimiting assertion type from assertion value. " +
            "If the literal text of your assertions contain commas or colons, please remove them, as such usage" +
            "is currently unsupported. If you experience this issue and feel that support for literal usage of these" +
            "characters in your assertions would be helpful, please file an issue: " +
            "https://github.com/PMA-2020/xform-test/issues/new" + excDetails;
        String invalidFieldExc = "In parsing assertions, an invalid field name was found. Valid field names are: " +
            validFields + excDetails;
        String valueAssertionExcNote= "Invalid assertion type: value assertion. Question/prompt '" + node.getName() +
                "' of type 'note' is read only. It is impossible to assert enterable value of assertion '%s'." +
                excDetails;
        String valueAssertionExcReadOnly = "Invalid assertion type: value assertion. Question/prompt '" +
                node.getName() + "' is read only. It is impossible to assert enterable value of assertion " +
                "'%s'." + excDetails;

        // single assertions & edge cases
        // The "ConstantConditions" inspection is wrong? Seems like a bug in IntelliJ upon my inspection.
        //noinspection ConstantConditions,StatementWithEmptyBody
        if (assertionsStr.length() == 0 && node.isRequired() && !(xlsformType(node).equals("note") || isReadOnly )) {
            throw new MissingAssertionError(missingAssertionError);
        } else if (numCommas == 0 && numColons == 0) {  // single un-named value assertion
            if (!assertionsStr.equals("")) {
                if (xlsformType(node).equals("note"))
                    throw new AssertionTypeException(String.format(valueAssertionExcNote, assertionsStr));
                else if (isReadOnly )
                    throw new AssertionTypeException(String.format(valueAssertionExcReadOnly, assertionsStr));
            }
            assertions.put("value", assertionsStr);
        } else if (numCommas == 0 && numColons > 0) {  // single named assertion
            assertionList.add(assertionsStr);

        // multiple assertions
        } else if (numCommas > 0 && numColons > 0) {
            if (numColons > numCommas + 1 || numColons < numCommas)
                throw new AssertionSyntaxException(commaColonExc);

            // split & trim
            if (assertionsStr.indexOf(',') != -1) {
                String[] unTrimmedAssertionList = assertionsStr.split(",");
                for (String assertion : unTrimmedAssertionList) {
                    assertionList.add(assertion.trim());
                }
            }
        }

        // build hashmap of assertions
        for (String assertion : assertionList) {
            String[] keyAndVal = assertion.split(":");
            String key = keyAndVal[0].trim();
            String val = keyAndVal[1].trim();
            if (key.endsWith("\\")) {  // check to see if they escaped a colon, i.e. "\:"
                key = "value";
                val = assertion.replace("\\:", ":");
            }
            String finalKeyToSatisfyOddJavaRule = key;
            boolean validField = validFields.stream().anyMatch(str -> str.trim().equals(finalKeyToSatisfyOddJavaRule));
            if (!validField)
                throw new AssertionSyntaxException(invalidFieldExc + "\n - Field: " + key);
            assertions.put(key, val);
        }
        thisRepeatInstanceAssertionStr = "";
        return normalizedAssertions(assertions);
    }

    /** Handles any edge cases in assertion text that would otherwise cause issues, by modifying said text.
     * @param assertions A collection of assertions which may or may not contain edge cases.
     * @return A collection of assertions.
     *
     * #to-do move any edge cases to a static map variable at top of class and loop through them here. */
    private static HashMap<String, String> normalizedAssertions(HashMap<String, String> assertions) {
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
        } else if (javarosaType.equals("MULTIPLE_ITEMS")) {  // #to-do Determine which is: select_one || select_multiple
            return String.valueOf(javarosaToXlsformTypeMapping.get(javarosaType));
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
            throw new NullPointerException("Node '" + node.getName() + "' was not found in form '" + formDef + "'.");
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
     * @param value The value expected.
     * @throws ValueAssertionError if value expected does not match value that exists on node. */
    @SuppressWarnings({"WeakerAccess"})  // #note-1 why are these warnings here? What if intend to be public?
    public static void assertValueMatches(TreeElement node, String value) throws ValueAssertionError {
        String nodeValue = node.getValue().getDisplayText();
        if (!nodeValue.equals(value))
            throw new ValueAssertionError("Asserted expected value '" + value + "' did not match actual value of node" +
                " '" + nodeValue + "' on node " + node.getName() + ".");
    }

    /** Asserts value evaluates as expected on a calculate.
     * @param node An XForm instance node representing an XLSForm calculate.
     * @param value The value expected to be evaluate correctly on node.
     * @throws ValueAssertionError if value doesn't evaluate as expected on node, for whatever reason. */
    private void assertCalculateEval(TreeElement node, String value) throws ValueAssertionError {
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
            throw new RuntimeException("Node passed to assertCaculateEval() does not represent an instance of an " +
                "XLSForm calculate.");

        if (!node.isRelevant())
            throw new ValueAssertionError("Could not evaluate calculate because relevant evaluated to false.");

        // to-do need not from calculate list but elsewhere?
        // to-do just pass it and get value?
        XPathConditional expression = calculateList.get(node.getName());
        EvaluationContext context = formEntryController.getModel().getForm().getEvaluationContext();
        DataInstance instance = formEntryController.getModel().getForm().getInstance();

        try {
            // not sure which of these is useful
            //noinspection UnusedAssignment
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
                throw new ValueAssertionError("Calculate '" + node.getName() + "' did not evaluate as expected." +
                        "\nExpected: " + value +
                        "\nGot: " + String.valueOf(result));
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
        String err = "Asserted enterable value '" + value + "' was not able to be entered on node "+node.getName()+".";
        if (!node.isRelevant())
            throw new ValueAssertionError(err + "\nThis is because the question relevant evaluated to false.");

        if ((xlsformType(node).equals("select_one") || xlsformType(node).equals("select_multiple"))
            && !validChoiceOption(node, value))
            throw new ValueAssertionError(err + " This choice option was not found in the list of options.");

        // convert to integer / decimal if needed
        boolean isLong = false;
        boolean isInt = false;

        try {
            Long.parseLong(value);
            isLong = true;
        } catch (Exception e) {
            try {
                Integer.parseInt(value);
                isInt = true;
            } catch (Exception e2) {
                // no-op
            }
        }

        int entryStatusCode;
        if (isInt) {
            entryStatusCode = formEntryController.answerQuestion(new IntegerData(Integer.parseInt(value)),
                    true);
        } else if (isLong) {
            entryStatusCode = formEntryController.answerQuestion(new LongData(Long.parseLong(value)), true);
        } else {
            entryStatusCode = formEntryController.answerQuestion(new StringData(value), true);
        }

        String entryStatus = formEntryControllerEntryStatusMapping.get(entryStatusCode);
        if (!entryStatus.equals("ANSWER_OK")) {
            if (entryStatus.equals("ANSWER_CONSTRAINT_VIOLATED")) {
                String constraintSyntaxTxt = (String) getPrivateProperty(node.getConstraint().constraint, "xpath");
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
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws RelevantAssertionError if relevant did not evaluate as expected. */
    private void handleRepeatProcessingRecursively() throws AssertionTypeException, AssertionSyntaxException,
            MissingAssertionError, RelevantAssertionError {
        boolean moreRepeatsRemain = currentRepeatNum <= currentRepeatTotIterationsAsserted;
        boolean repeatNodesInInstanceHaveBeenCheckedOnce = currentRepeatNum > 1;
        if (repeatNodesInInstanceHaveBeenCheckedOnce && moreRepeatsRemain)
            processNodes(currentRepeatNodes);
    }

    /** Handles repeat group nodes
     *  - Keeps track of node in repeat, for further iteration if necessary.
     *  - Validates repeat assertion syntax, e.g. "[<assertions>, <assertions, ...]".
     *
     * @param node Current node being executed.
     * @throws AssertionSyntaxException If inconsistent number of repeat instances implicitly asserted. */
    private void trackAndValidateRepeatNode(TreeElement node) throws AssertionSyntaxException {
        // track - keep track of all nodes in repeat, for further iteration if necessary
        currentRepeatNodes.add(node);

        // validate - check for any inconsistencies in number of repeat group tests to be asserted
        String nodeAssertionStr = node.getBindAttributeValue(null, "xtest-linearAssert");
        if (nodeAssertionStr.charAt(0) == ('[')) {
            int numIterationsAsserted = Math.toIntExact(nodeAssertionStr.chars().filter(num -> num == ',').count() + 1);
            if (currentRepeatTotIterationsAsserted == 0)
                currentRepeatTotIterationsAsserted = numIterationsAsserted;
            else if (currentRepeatTotIterationsAsserted != numIterationsAsserted)
                throw new AssertionSyntaxException("An inconsistent number of repeat instances were implicitly " +
                    "asserted. Check each question's assertion syntax (e.g. [<assertions>, <assertions, ...]) and " +
                    "make sure they all have the same number of commas.");
        }
    }

    /** Proceed forward one step in the form.
     * Side effects: Updates 'currentEventState' to reflect the state of the form reported by FormEntryController. */
    private void step() {
        int stepResult = formEntryController.stepToNextEvent();
        currentEventState = formEntryControllerEventStatusMapping.get(stepResult);
    }

    /** Runs assertion test on asserted values for a given node, and updates running testCases for report.
     * @param node Instance XForm node containing XFormTest assertion bind.
     * @param value A value assertion on the XFormTest assertion bind. */
    private void handleValueAssertion(TreeElement node, String value) {
        HashMap<String, String> nodeTestResults = new HashMap<>();
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
    private void handleRelevantAssertion(TreeElement arbitraryNode, String relevant) throws AssertionSyntaxException, RelevantAssertionError {
        HashMap<String, String> nodeTestResults = new HashMap<>();
//        FormEntryModel model = formEntryController.getModel();
//        FormIndex index = model.getFormIndex();
//        TreeElement activeInstanceNode = formDef.getMainInstance().resolveReference(index.getReference());
        // TODO: fix: Won't be active instance node if not relevant
        // TODO: why would arbitraryNode (uninstantiated) be == to instantiated node?
        TreeElement node = formDef.getMainInstance().resolveReference(arbitraryNode.getRef());

        boolean relevantSyntaxValid = (relevant.equals("true") || relevant.equals("1") ||
            relevant.equals("false") || relevant.equals("0"));
        if (!relevantSyntaxValid)
            throw new AssertionSyntaxException("Relevant statement syntax on'" + node.getName() + "' was invalid." +
                "\nExpected: " + "true || 1 || false || 0" +
                "\nGot: " + relevant);
        boolean assertedIsRelevant = (relevant.equals("true") || relevant.equals("1"));
        if (node.isRelevant() != assertedIsRelevant)
            throw new RelevantAssertionError("Relevant for '" + node.getName() + "' did not evaluate as expected." +
                "\nExpected: " + String.valueOf(assertedIsRelevant) +
                "\nGot: " + String.valueOf(node.isRelevant()));
        nodeTestResults.put("relevant", node.getName());
        nodeTestResults.put("relevantAssertion", String.valueOf(node.isRelevant()));
        testCases.add(nodeTestResults);
    }

    /** Tests assertions on a given node.
     * @param node The TreeElement instance node which contains xform-test bind information.
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws RelevantAssertionError if relevant did not evaluate as expected. */
    private void testAssertions(TreeElement node) throws AssertionTypeException, AssertionSyntaxException,
            MissingAssertionError, RelevantAssertionError {
        HashMap<String, String> assertions = nodeAssertions(node);

        if (!assertions.get("value").equals(""))
            handleValueAssertion(node, assertions.get("value"));
        if (!assertions.get("relevant").equals(""))
            handleRelevantAssertion(node, assertions.get("relevant"));
    }

    /** Iterate through nodes and run tests on them.
     * @param arbitraryNodeset Nodes to iterate through. These could for example be the set of all instance nodes in an
     * XForm, or it might just be the set of all nodes in a given repeat group. These nodes may or may not be tied to
     * an active instance in a running form. In actuality, they probably are not.
     * @return True if finished as expected, else false.
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws RelevantAssertionError if relevant did not evaluate as expected. */
    private boolean processNodes(ArrayList<TreeElement> arbitraryNodeset) throws AssertionSyntaxException,
            AssertionTypeException, MissingAssertionError, RelevantAssertionError {
        HashMap<String, Integer> nodeIndexLookup = new HashMap<>();
        for (int i=0; i<arbitraryNodeset.size(); i++) {
            nodeIndexLookup.put(arbitraryNodeset.get(i).getName(), i);
        }

        for (TreeElement node : arbitraryNodeset) {
            TreeElement activeInstanceNode = formDef.getMainInstance().resolveReference(
                formEntryController.getModel().getFormIndex().getReference());
            int arbitraryNodesetIndex = nodeIndexLookup.get(node.getName());
            int activeInstanceIndex = nodeIndexLookup.get(activeInstanceNode.getName());

            if (currentRepeatNum == 1)  // Only needed once per repeat group.
                trackAndValidateRepeatNode(node);
            testAssertions(node);
            if (activeInstanceIndex == arbitraryNodesetIndex)  // this may need work
                step();
            if (currentEventState.equals("EVENT_PROMPT_NEW_REPEAT")) {
                handleRepeatNavigation();
                handleRepeatProcessingRecursively();
            }

            // TODO 5.5: PR JavaRosa docstrings.
            // TODO 6: Build and release
        }
        //noinspection UnnecessaryLocalVariable
        boolean finishedAsExpected = currentEventState.equals("EVENT_END_OF_FORM");
        return finishedAsExpected;
    }

    /** Runs a XFormTest 'linear assertion test'.
     *
     * # Background
     * XForm-test introduces a specific kind of test called the ‘linear scenario test’. A linear scenario test defined
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
     * @throws AssertionSyntaxException if invalid syntax via usage of comma within values rather than exclusively as an
     * assertion field delimiter.
     * @throws AssertionTypeException if invalid assertion type (e.g. value or relevant or constraint) is
     * asserted. This can happen if, for example, a value assertion is made on a read_only question. For read_only
     * questions, no value can be entered, so such an assertion is inherently invalid.
     * @throws MissingAssertionError if no assertions on non-read only , required question prompts.
     * @throws RelevantAssertionError if relevant did not evaluate as expected.
     *
     * #to-do: Need to try this only if detecting that it is a repeat group first.
     * #to-do: error out on currently unsupported assertions
     * #to-do: allow for assertions on currently unsupported XLSForm types: calculate
     *   May need to backtrack and check every node that hasn't occurred and check it? formEntryController.getModel();
     * #to-do: constraint assertions
     *   boolean constraintEval = form.evaluateConstraint(TreeReference ref, IAnswerData data);
     *   TreeElement .setValue() && .constraint || .getConstraint()?
     * #to-do: relevant assertions
     *   boolean relevantEval = state.isIndexRelevant();
     * */
    private void linearAssertionTest() throws AssertionSyntaxException, AssertionTypeException,
            MissingAssertionError, RelevantAssertionError {
        String testType = "linearAssertionTest";
        instanceNodes = instanceNodes(formEntryController);
        formElements = instanceNodesToFormElements(instanceNodes);

        System.out.println("2. Running test: linear assertion test");
        step();
        boolean success = processNodes(formElements);

        String exitCode = success ? "0" : "1";
        results.put("exitCode", exitCode);
        printResults(testType, results);
        exit(Integer.valueOf(results.get("exitCode")));
    }
}
