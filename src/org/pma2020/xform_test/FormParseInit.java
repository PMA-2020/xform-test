package org.pma2020.xform_test;

import org.javarosa.core.model.FormDef;
import org.javarosa.xform.util.XFormUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * This class sets up everything you need to perform tests on the models and form elements found in JR (such
 * as QuestionDef, FormDef, Selections, etc).  It exposes hooks to the FormEntryController,FormEntryModel and
 * FormDef (all the toys you need to test IFormElements, provide answers to questions and test constraints, etc)
 */
class FormParseInit {
    private final String FORM_NAME;
    private FormDef xform;

    FormParseInit(Path form) {
        FORM_NAME = form.toString();
        this.init();
    }

    private void init() {
        String xf_name = FORM_NAME;
        FileInputStream is;
        try {
            is = new FileInputStream(xf_name);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error: the file '" + xf_name + "' could not be found!");
        }

        // Parse the form
        xform = XFormUtils.getFormFromInputStream(is);

        if (xform == null) {
            throw new RuntimeException("ERROR: XForm has failed validation!");
        }
    }

    /**
     * @return the FormDef for this form
     */
    FormDef getFormDef() {
        return xform;
    }
}
