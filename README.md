# XFormTest
[XFormTest](http://xform-test.pma2020.org) is a tool for creating and running tests to ensure the quality and stability 
of XForms.

# Quick start
1. Prerequisites: i. Java 8, ii. access to an XLSForm (excel) to XForm (xml) converter, such as [pyxform](
https://github.com/XLSForm/pyxform) or [XLSForm online](https://opendatakit.org/xlsform/), or [XLSForm offline](
https://github.com/opendatakit/xlsform-offline/releases). 
2. Download [XFormTest](https://github.com/PMA-2020/xform-test/releases).
3. Download [this example XLSForm](
https://github.com/PMA-2020/xform-test/raw/master/docs/source/_static/xlsxExample.xlsx) and convert it to XML.
4. Run the pre-made tests in the example form (replace "x.y.z" with the version number in the file name of the Java jar
file downloaded): `java -jar xform-test-x.y.z.jar xlsxExample.xml`

Quick start video: https://www.youtube.com/watch?v=doAr26GaTSQ

# Installation
XFormTest does not need to be installed. You simply have to have Java 8 in order to run it.
 
#### Pre-requisities
- [Java JDK 8+](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

#### Download
You can download XFormTest from the [GitHub release page](https://github.com/PMA-2020/xform-test/releases).

# Basic Usage
#### Command syntax
`java -jar path/to/xform-test-x.y.z.jar path/to/someFile.xml`

#### Example usage
If you've downloaded version 0.1.0 of XFormTest and an XForm XML file called "someFile.xml", and both of these are in 
the same folder, you would open up your terminal at that folder and run the following command: 
`java -jar xform-test-0.1.0.jar someFile.xml`

# Creating tests
<!--
## Traditional unit tests
Support for traditional one-off unit test cases and test suites is planned for at a later date after initial release. 
It is currently marked for [release 1.2](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0)
## The _linear scenario_ test
-->

XFormTest introduces a concept we call the "linear scenario test". A _linear scenario test_ defined as a set of 
assertions to be executed in a sequence, one after the other.

<!--
The reason it can be important to test a set of questions answers or assertions linearly is due to 
interdependencies. In forms of any non-trivial complexity, there exist many questions which are dependent on other 
questions. Therefore, it is important useful test whole scenarios, rather than individual questions in isolation.

It is also possible that a single survey might be very different depending on different scenarios. Imagine, for 
example, that you are designing a family planning survey. The main target audience for the survey is women of 
reproductive age. However, it could be that the survey workflow and the specific questions that are asked differ 
greatly depending on various factors, such as (1) whether or not the respondent has used a particular method of family 
planning, or (2) whether or not the respondent has ever had a live birth. Or, there may be alternative questions asked 
even if the respondent is not within the target audience, that is, in this case, (3) not being a woman of preproductive 
age. The examples (1), (2), and (3) describe very different _scenarios_ where the survey workflow and the types of 
questions asked might be very different. Therefore if testing such a form, it might be prudent to test these 3 
different scenarios individually.
-->

### XLSForm setup
For a quick idea of how this works in an XLSForm, check out the example below. There's also a demo form available for 
download [here](https://github.com/PMA-2020/xform-test/raw/master/docs/source/_static/xlsxExample.xlsx).

![xlsx_example.png](https://github.com/PMA-2020/xform-test/raw/master/docs/source/_static/xlsxExample.png)
_An example XLSForm with tests._


#### Adding the XFormTest column
XFormTest utilizes special columns for linear assertion. Any of the following 3 column names are valid.
- `bind::xform-test-linearAssert`
- `bind::xform-test`
- `bind::test`

<!--
Either (1), (2), (3) above, but without the "bind::" _(not in current release)_.)
-->

<!--
The `bind::` portion of the column names is a directive which tells the XLSForm to XForm converter (an intermediate 
steps before tests are run) to place the test attribute onto the `<bind>` element corresponding to the XForm node 
(which is the XML Xform corollary for a particular question/prompt row in an XLSForm). More information about _bindings_
 can be found in the [ODK XForms spec documentation](https://opendatakit.github.io/xforms-spec/#bindings).

The naming option labeled (3), `test`, is the most readable. However, it may not always be possible to use this name 
due to the possibility of other unit test libraries or bespoke tests which might use the same attribute name. The 
naming option labeled (2), `xform-test`, is namespaced. This avoids naming collisions. However, 
[future releases of xform-test](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0) are expected to release 
alternate test types (e.g. unit tests). By default, a column named `xform-test` will be interpreted as a _linear 
scenario_ test. The naming option labeled (1), `xform-test-linearAssert` is the most explicit. Nonetheless, XForm-test 
will interpret any of these column names as valid attributes for a _linear scenario_ test as of [version 1.0](
https://github.com/PMA-2020/xform-test/releases/tag/1.0.0).
-->

Any text entered into the cells of this column are considered "assertions". An "assertion" represents something about 
the particular question or prompt that you expect to be true. In the context of a _linear scenario_ test, 
the assertion states something expected to be true not just in isolation, but also true following _every other_ 
assertion made in the form up to that point.

<!--
### XForm setup
Setting up a _linear test scenario_ test in an XForm is simple. Given an already complete XForm, linear assertions can 
be added to the `<bind>` element pertaining to a particular `nodeset`. Take the following, for example.

`<bind nodeset="/XformTest1/HH_selectionrpt/HH_selectiongrp/structure" required="true()" type="int" xtest-linearAssert="
[123, 999]"/>`

In this example, there is a form called `XformTest1`. Within that form, there is a repeat group called `HH_selectionrpt`
. Within that, there is a question group called `HH_selectiongrp`. Within that, there is a particular question / node 
called `structure`. The `<bind>` element in this example contains typical attributes for an XForm, such as `required` 
and `type`. Additionally, a _linear scenario test_ assertion has been made by adding an attribute called 
`xtest-linearAssert`, the value of which is an array `[123, 999]`. Since this is an array of length 2, we can infer 
that the repeat group was iterated through 2 times. In the first iteration, the value entered for `structure` was `123`
. In the second repeat iteration, the value entered was `999`.
-->

#### Adding assertions
There are 2 types of assertions: _value assertions_, and _relevant assertions_.

**Value assertions** indicate that the user is able to enter the given specified value for a given question. In a future 
release of XFormTest, we are also planning to allow assertions on calculates.

**Relevant assertions** allow you to simply assert whether a given question/prompt is relevant or not.  

|Assertion type |XLSForm examples     |XForm example
|--------------|---------------------|-------------
|Value         |`Bob` or `value: Bob`|`<bind ... xform-test-linearAssert="Bob"/>`
|Relevant      |`relevant: 0`        |`<bind ... xform-test-linearAssert="relevant: 0"/>`


<!--
|Constraint    |constraint: 0    |<bind ... xform-test-linearAssert="constraint: 0"/>
-->

#### Multiple tests in a single form
Support for multiple linear scenario tests (e.g. multiple columns in a single XLSForm) is not currently available, but 
planned to be implemented in [a future release](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0).  

<!--
Support for multiple linear scenario tests within a single form is planned for at a later date after initial release. 
For XLSForms, the plan is to have multiple `xform-test` columns, one for each scenario. For XForms, it is planned to 
have multiple `xform-test` attributes on single `<bind>` element for a given node. It is currently marked for 
[release 1.2](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0)
-->

# Syntax reference
## Expressions
`[nodeset attribute-1]: [nodeset attribute-1-evaluated-value], ...`

Quick example: `relevant: 1, value: yes, constraint: 0`

## Assertion types
#### Value
A `value` assertion is the assertion that some `value` can be entered for a given question / node. The data type for a 
`value` assertion depends on the question / node `type`. A `text` type would accept string values, an `integer` type 
would accept integer values, a `calculate` type could accept any kind of data value, and so on.

For example, for a quesiton such as "How many days a week is this facility open?", one might want to value assertion of 
a number 1 through 7.

#### Relevant
A `relevant` assertion is an assertion on the evaluation of an expression set on the `relevant` attribute of a given 
question / node. It accepts a boolean of one of the following forms: `false`, `true`, `0` or `1`.

For example, if there are a particular question has complex pre-conditions, one might want to create a linear scenario 
test where, after these pre-conditions are met, the `relevant` of the questions hould evaluate to `1`.

<!--
#### Constraint
A `constraint` assertion is an assertion on the evaluation of an expression set on the `constraint` attribute of a 
given question / node. It accepts a boolean of one of the following forms: `false`, `true`, `0` or `1`. An assertion of 
`true` assumes that the input on a question / node is invalid, i.e. the constraint "fires". An assertion of `false` 
assumes that the input on a question / node is valid, i.e. the constraint "does not fire".

#### Choice Filter
Asserts that a `choice_filter` acts as expected. This feature is planned for implementation at a later date after 
initial release--currently marked for [release 1.1](https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

#### Label`[::<language>]`
Asserts that a `label` renders as expected. Useful when the field renders dynamically based on variable interpolation. 
This feature is planned for implementation at a later date after initial release--currently marked for [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

#### Hint`[::<language>]`
Asserts that a `hint` renders as expected. Useful when the field renders dynamically based on variable interpolation. 
This feature is planned for implementation at a later date after initial release--currently marked for [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).
-->

<!--
# Running tests
## Quick start for running a test on an XML XForm
`xform-test path/to/file.xml`

## Quick start for running a test on an Excel-based XLSForm
`xform-test path/to/file.[xls|xlsx]`
-->

<!--
# Interpreting test results
## Test result format
Test results are returned in JSON. Syntax is as followed.
```
{
    "status": "[ PASS | FAIL ]",
    "exitCode": [ 0 | 1 ],
    "statusCode": [0 | 1.x],
    "details": {
        "nodeId": !String
        "label": !String,
        "pointOfFailure": !String,
        "failedAssertionExpression": !String
    },
    "message": !String
}
```

Here is a further breakdown of the test result fields.

- `status`: Always present. String. Will be `"PASS"` or `"FAIL"`.
- `exitCode`: Always present. Number. Will be `0` for passing test or `1` for failing test.
- `statusCode`: Always present. Number. Will be `0` for passing test or `1.x` for failing test, where `x` is an integer 
representing the specific numeric code of the failure case.
- `details`: Always present. Object or null. Details where and why the test failed.
    - `nodeId`: Always present on failure. String. Represents the XForm element node name within `<instance>`. In an 
    XLSForm, this is the `name` field of a given row.
    - `label`: Always present on failure. String or null. The language used for the label will be the default language 
    of the form, if specified.
    - `pointOfFailure`: Always present on failure. String. Will either be (a) the name of the node attribute containing 
    expression of failure, e.g. `relevant`, `constraint`, `choice_filter`, (b) `value` if a value input was asserted, 
    or (c) `choice list` if an available choice list was asserted--whichever is applicable.
    - `failedAssertionExpression`: Always present on failure. String. Will be either (a) the literal expression of the 
    attribute of failure, (b) an asserted value input, or (c) asserted available choice list--whichever is applicable.
- `message`: Always present. String. A message describing in one or more sentences what happened, where it happened, 
and occasionally other information.


## Example of passing test result
```json
{
    "status": "PASS",
    "exitCode": 0,
    "statusCode": 0,
    "details": null,
    "message": "Success"
}
```

## Interpreting failing tests
This section details all of the various cases of test failures, and how to interpret them. The number at the front of 
each of the following sub-headers represents the designated `statusCode` for the particular failure case.  For each 
provided case, there is provided an example of the test assertion made, as well as an example of the test result text 
which would return upon failure.

### 1.1 When mock `value` can't be inserted due to `constraint`
|Assertion type|XLSForm example  |XForm example
|--------------|-----------------|-------------
|value         |15               |<bind ... xform-test-linearAssert="15"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.1,
    "details": {
        "nodeId": "age",
        "label": "Enter the respondent's age.",
        "pointOfFailure": "constraint",
        "failedAssertionExpression": ". > 18"
    },
    "message": "Linear scenario test failed on node 'age'. Value assertion '15' was made, but this violated the constraint: '. > 18'."
}
```

### 1.2 When mock `value` can't be inserted due to `relevant`
|Assertion type|XLSForm example  |XForm example
|--------------|-----------------|-------------
|value         |yes              |<bind ... xform-test-linearAssert="yes"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.2,
    "details": {
        "nodeId": "abortion_attempt_yn",
        "label": "Have you ever done anything to stop a pregnancy or bring back your period?",
        "pointOfFailure": "relevant",
        "failedAssertionExpression": "${has_been_pregnant} = 'yes'"
    },
    "message": "Linear scenario test failed on node 'abortion_attempt_yn'. A value assertion was made, but value could be entered due to 'relevant' having evaluated to 'false' in the expression: '${has_been_pregnant} = 'yes''. Please check logic and assertions leading up to 'abortion_attempt_yn'."
}
```

### 1.3 When mock `value` can't be inserted due to `relevant` on ancestor node
|Assertion type|XLSForm example  |XForm example
|--------------|-----------------|-------------
|value         |Jane             |<bind ... xform-test-linearAssert="Jane"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.3,
    "details": {
        "nodeId": "recent_birth_name",
        "label": "What is the name of your most recent child?",
        "pointOfFailure": "relevant",
        "failedAssertionExpression": "${recent_birth_result} = 'live_birth'"
    },
    "message": "Linear scenario test failed on node 'recent_birth'. A value assertion was made, but value could not be entered due to 'relevant' evaluating to 'false' for ancestor node 'pregnancy_history_question_group'. The 'relevant' expression for the ancestor node was: '${has_been_pregnant} = 'yes''. Please check logic and assertions leading up to 'pregnancy_history_question_group'."
}
```

### 1.4 When mock `value` can't be inserted due to `choice_filter`
This feature is planned for implementation at a later date after initial release--currently marked for [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

### 1.5 Failure of `relevant` assertion
|Assertion type|XLSForm example  |XForm example
|--------------|-----------------|-------------
|relevant      |relevant: 0      |<bind ... xform-test-linearAssert="relevant: 0"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.5,
    "details": {
        "nodeId": "phone_number",
        "label": "What is your phone number?",
        "pointOfFailure": "relevant",
        "failedAssertionExpression": "${phone_number_permission} = 'yes'"
    },
    "message": "Linear scenario test failed on node 'phone_number'. An assertion was made that the 'relevant' should evaluate to 'false', but it evaluated to 'true' in the expression: '${phone_number_permission} = 'yes''. Please check logic and assertions leading up to 'phone_number'."
}
```

### 1.6 Failure of `constraint` assertion
|Assertion type| XLSForm example       | XForm example
|---	       |---	                   |---
|constraint    |value: 5, constraint: 0|<bind ... xform-test-linearAssert="value: 5, constraint: 0"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.6,
    "details": {
        "nodeId": "days_per_week_open",
        "label": "How many days a week is this facility open?",
        "pointOfFailure": "constraint",
        "failedAssertionExpression": ". >= 7"
    },
    "message": "Linear scenario test failed on node 'days_per_week_open'. An assertion was made that 'constraint' should evaluate to 'false' meaning that value entered in the assertion should have been valid. However, the constraint evaluated to 'true' in the expression: '. >= 7' for value '5'."
}
```

In the above example, we can see that the cause of failure was an accidental flipping of less than / greater than.

### 1.7 Failure of `choice_filter` assertion
This feature is planned for implementation at a later date after initial release--currently marked for [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

### 1.8 Failure of asserted list of `choices`
This feature is planned for implementation at a later date after initial release--currently marked for [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

# CLI Reference
## Syntax
`xform-test path/to/file [options]`

## Options
| Short Flag | Long Flag | Description
|------------|-----------|------------
| -h         | --help    |Access the help file.
| -o         | --output  |Designated a specific output directory. Should be of the form `-o path/to/dir`. 
-->

# Miscellaneous
#### Useful resources
**General resources**
- [XForm Test documentation](http://xform-test.pma2020.org)
- [XForm Test on GitHub](https://github.com/PMA-2020/xform-test/)
- [Open Data Kit](https://opendatakit.org/) - An open source data collection platform utilizing XForms.
- [PMA2020](https://www.pma2020.org/) - A programmatic data collection organization for rapid-turnaround surveys 
monitoring key health and development indicators. Birth place of _XForm Test_.
- [PMA2020 on GitHub](https://www.github.com/PMA-2020) - Open source projects and initiatives by PMA2020.

**Other XML and XForm based testing projects**
- [XMLUnit](https://github.com/xmlunit/xmlunit) - An XML language native, platform agnostic library for comparing XML 
documents, schema validation, and XPath value assertion
- [XForms Unit](http://eric.van-der-vlist.com/blog/2013/08/20/xforms-unit/) - a non-implemented unit testing library 
specification
- [Orbeon Testsuite](https://github.com/nvdbleek/com.orbeon.testsuite.w3c) - A selenium-based test runner specifically 
for Orbeon
- [XForms Client-Side Unit Tests](http://wiki.orbeon.com/forms/doc/contributor-guide/xforms-unit-tests) - Another 
Orbeon-specific test suite for unit tests implemented in JavaScript

#### Giving feedback or reporting bugs
Send an e-mail to jflack@jhu.edu, or, even better, [write an issue on GitHub](
https://github.com/PMA-2020/xform-test/issues/new).

#### Collaboration
[XForm-test](https://github.com/PMA-2020/xform-test) is forkable. Pull requests welcome!

#### Getting help
Send an e-mail to jflack@jhu.edu or [write an issue on GitHub](https://github.com/PMA-2020/xform-test/issues/new). For 
now, live, real time assistance is also available on Skype. Send a message to: `joeflack4`.


---


# XFormTest
[XFormTest](http://xform-test.pma2020.org) est un outil permettant de créer et d’exécuter des tests pour assurer la qualité et stabilité des
XForms.

# Démarrage rapide
1. Prérequis : i. Java 8, ii. Accès à un convertisseur de XLSForm (Excel) vers XForm (xml), comme par exemple [pyxform](https://github.com/XLSForm/pyxform) ou [XLSForm online](https://opendatakit.org/xlsform/), ou encore [XLSForm offline](https://github.com/opendatakit/xlsform-offline/releases).
2. Télécharger [XFormTest](https://github.com/PMA-2020/xform-test/releases).
3. Télécharger [cet exemple XLSForm](https://github.com/PMA-2020/xform-test/raw/master/docs/source/_static/xlsxExample.xlsx) et convertissez-le en XML.
4. Exécutez les tests pré-créés dans l’exemple de formulaire (remplacez "x.y.z" par le numéro de version dans le nom du fichier Java jar téléchargé) : `java -jar xform-test-x.y.z.jar xlsxExample.xml`

Vidéo : https://www.youtube.com/watch?v=doAr26GaTSQ

# Installation
XFormTest n’a pas besoin d’être installé. Vous devrez simplement avoir Java 8 pour l’exécuter.

#### Prérequis
- [Java JRE 8+](http://www.oracle.com/technetwork/java/javase/downloads/)

#### Téléchargement
Vous pouvez télécharger XFormTest sur [page de publication de GitHub](https://github.com/PMA-2020/xform-test/releases).

# Usage de base
#### Syntaxe de commande
`java -jar path/to/xform-test-x.y.z.jar path/to/someFile.xml`

#### Exemple d’usage
Si vous avez téléchargé la version 0.1.0 de XFormTest et le fichier XForm XML appelé "someFile.xml", et que les deux se trouvent dans le même dossier, vous devrez ouvrir votre terminal pour ce dossier et exécuter la commande suivante :
`java -jar xform-test-0.1.0.jar someFile.xml`

# Créer des tests
<!--
## Tests unitaires traditionnels
Il est prévu de permettre les tests unitaires traditionnels ponctuels et les suites de tests ultérieurement après la version initiale.
Actuellement indiquée comme [release 1.2](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0)
## The _linear scenario_ test
-->

XFormTest introduit un concept que nous appelons le « test du scénario linéaire » (« linear scneario test », en anglais). Un _linear scenario test_ est défini comme un ensemble d’assertions à exécuter par séquence, l’une après l’autre.

<!--
La raison pour laquelle il peut être important de tester un ensemble de réponses ou d’assertions de manière linéaire est pour en vérifier leurs interdépendances. Dans les formulaires complexes, de nombreuses questions dépendent les unes des autres. Par conséquent, il est important et utile de tester des scénarios dans leur ensemble, plutôt que des questions isolées.

Il est également possible qu’une même enquête puisse être très différente selon différents scénarios. Imaginez, par exemple, que vous concevez une enquête de planification familiale. Le public cible principal de cette enquête sont les femmes en âge de procréer.
Cependant, il se pourrait être que le déroulement de l’enquête et les questions spécifiques que vous posez diffèrent grandement selon divers facteurs, comme par exemple
(1) si la répondante a utilisé une méthode de planification familiale en particulier, ou
(2) si la répondante a déjà accouché d’un enfant vivant (naissance vivante). D’autres questions alternatives pourraient aussi être posées même si la répondante ne se trouve pas dans le public cible, soit dans ce cas,
(3) si elle n’est pas une femme en âge de procréer.
Les exemples (1), (2), et (3) décrivent des scénarios très différents, selon lesquels le déroulement de l’enquête et les types de questions peuvent varier significativement. Ainsi, si vous testez le questionnaire de cette enquête, il serait prudent de tester ces 3 scénarios individuellement.
.
-->

### Paramétrage de XLSForm
Pour savoir rapidement en quoi cela consiste dans un XLSForm, référez-vous à l’exemple ci-dessous. Une démo est aussi disponible en téléchargement
[ici](https://github.com/PMA-2020/xform-test/raw/master/docs/source/_static/xlsxExample.xlsx).

![xlsx_example.png](https://github.com/PMA-2020/xform-test/raw/master/docs/source/_static/xlsxExample.png)
_Un exemple de XLSForm avec des tests._


#### Ajouter des colonnes dans XFormTest
XFormTest utilise des colonnes spéciales pour des assertions linéaires. Les 3 noms de colonnes suivants sont tous valides.
- `bind::xform-test-linearAssert`
- `bind::xform-test`
- `bind::test`

<!--
Soit (1), soit (2), soit (3) ci-dessus, mais sans "bind::" _(actuellement pas dans la publication )_.)
-->

<!--
La portion `bind::` des noms de colonnes ordonne au convertisseur de fichiers XLSForm en XForm (étape intermédiaire avant que
les tests ne soient exécutés) de placer l’attribut du test sur l’élément `<bind>` correspondant au node XForm
(qui est le corollaire XML Xform d’une question/relance en particulier dans un formulaire XLSForm). Plus d’informations sur les _bindings_
 sont disponibles sur [ODK XForms spec documentation](https://opendatakit.github.io/xforms-spec/#bindings).

L’option d’appellation (3), `test`, est la plus lisible. Cependant, il n’est pas toujours possible d’utiliser ce nom car d’autres répertoires (« libraries ») de tests unitaires ou de tests sur mesure pourraient déjà utiliser le même nom d’attribut.
L’option d’appellation (2), `xform-test`, comporte des espaces de noms. Cela évite les collisions d’appellation. Cependant, [les prochaines versions du xform-test](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0) devraient publier des types de tests alternatifs
(ex : tests unitaires). Par défaut, une colonne nommée `xform-test` sera interprétée comme un  _linear scenario_ test.
L’option d’appellation (1), `xform-test-linearAssert` est la plus explicite. Néanmoins, XForm-test
interprétera tous les noms de cette colonne comme des attributs valides pour un _linear scenario_ pour la [version 1.0](
https://github.com/PMA-2020/xform-test/releases/tag/1.0.0).
-->

Tout texte saisi dans les cellules de cette colonne sont considérés comme des "assertions". Une "assertion" représente ce que vous attendez d’une question ou relance comme étant vrai. Dans le contexte d’un test de scénario linéaire, l’assertion établit quelque chose qui devrait être vrai, pas simplement de manière isolée, mais aussi en suivant toutes les autres assertions apportées au formulaire jusqu’à présent.




<!--
### Paramétrage du XForm
Paramétrer un test de scénario linéaire dans un XForm est simple. Dans les XForm déjà remplis, les assertions linéaires peuvent être ajoutées à l’élément
`<bind>` correspondant à un `nodeset` en particulier. En voici un exemple :

`<bind nodeset="/XformTest1/HH_selectionrpt/HH_selectiongrp/structure" required="true()" type="int" xtest-linearAssert="
[123, 999]"/>`

Dans cet exemple, un formulaire est nommé `XformTest1`. Dans ce formulaire, il y a un groupe de répétition appelé `HH_selectionrpt`
Dedans, il y a un groupe de questions appelé `HH_selectiongrp`. Dans cela, il y a une question/node spécifique appelé
`structure`. L’élément `<bind>` dans cet exemple contient les attributs typiques d’un XForm, tels que `required` et `type`. Par ailleurs, une assertion d’un test de scénario linéaire a été créée en ajoutant un attribut appelé
`xtest-linearAssert`, dont la valeur est la fourchette `[123, 999]`. Puisqu’elle est de longueur 2, nous pouvons en déduire que le groupe de répétition a été itéré 2 fois.
Dans la première itération, la valeur saisie pour `structure` était`123`
Dans la deuxième itération, la valeur saisie était `999`.
-->

#### Ajouter des assertions
Il y a 2 types d’assertions :  _value assertions_, et _relevant assertions_.

**Value assertions** indique que l’utilisateur est en mesure de saisir une valeur spécifique pour une question donnée. Dans la prochaine version du XFormTest, nous prévoyons de permettre des assertions sur des calculs également.

**Relevant assertions** vous permet d’affirmer simplement si une question/relance est pertinente ou non.

|Type d’assertion | Exemple de  XLSForm | Exemple de XForm
|--------------|---------------------|-------------
|Valeur         |`Bob` ou `value: Bob`|`<bind ... xform-test-linearAssert="Bob"/>`
|Relevant      |`relevant: 0`        |`<bind ... xform-test-linearAssert="relevant: 0"/>`


<!--
|Constraint    |constraint: 0    |<bind ... xform-test-linearAssert="constraint: 0"/>
-->


#### Tests multiples dans un seul formulaire
Les tests de scénarios linéaires multiples (ex : plusieurs colonnes dans un seul XLSForm) ne sont pas disponibles actuellement, mais prévus pour
 [de prochaines versions](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0).

<!--
Il est prévu de permettre les tests de scénarios linéaires multiples dans un seul formulaire ultérieurement, après la version initiale.
Pour les XLSForms, il est prévu d’avoir plusieurs colonnes `xform-test`, une pour chaque scénario. Pour les XForms, il est prévu d’avoir plusieurs attributs `xform-test` sur un seul élément `<bind>` pour un node donné. Cela est actuellement indiquée comme
[release 1.2](https://github.com/PMA-2020/xform-test/releases/tag/1.2.0)
-->

# Syntaxe de référence
## Expressions
`[nodeset attribute-1]: [nodeset attribute-1-evaluated-value], ...`

Exemple : `relevant: 1, value: oui, constraint: 0`

## Types d’assertion
#### Value
Une assertion `value` (de valeur) est l’assertion selon laquelle une certaine `value` peut être saisie pour une question donnée. Le type de donnée pour une assertion
`value` dépend du `type` de question/ node. Un type `text` n’accepterait que des valeurs de caractères, et un type `integer` n’accepterait que des valeurs numériques, tandis qu’un type `calculate` pourrait accepter tout type de valeur de données, et ainsi de suite.

Par exemple, pour une question telle que « Combien de jours par semaine cette structure sanitaire est-elle ouverte ? », on pourrait avoir l’assertion de valeur du chiffre 1 à 7.

#### Relevant
Une assertion `relevant` (pertinente) est une assertion d’évaluation d’un ensemble d’expressions comme était `relevant` pour une question/node. Elle accepte
un Boolean sous l’une des formes suivantes : `false`, `true`, `0` or `1`.

Par exemple, si une question en particulier a des préconditions complexes, on pourrait vouloir créer un test de scénario linéaire selon lequel, une fois les préconditions remplies, l’assertion `relevant` des questions devrait être évaluée à `1`.

<!--
#### Constraint
Une assertion `constraint` (de contrainte) est une assertion d’évaluation d’un ensemble d’expressions comme étant `constraint` pour une question/node. Elle accepte
un Boolean sous l’une des formes suivantes : `false`, `true`, `0` or `1`. Une assertion
`true` présume que les données saisies pour la question/node sont invalides, c’est-à-dire que la contrainte « déclenche ». Une assertion `false` présume que les données saisies pour la question/node est valide, soit que la contrainte « ne déclenche pas ».

#### Choice Filter
Affirme qu’un `choice_filter` (filtre de choix) agit comme prévu. Cette caractéristique est prévue pour une date ultérieure, après la version initiale
--actuellement indiquée comme [release 1.1](https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

#### Label`[::<language>]`
Affirme qu’un `label` donne le résultat attendu. Utile lorsqu’une le champ se remplit de manière dynamique en fonction d’une interpolation de variables.
Cette caractéristique est prévue pour une date ultérieure, après la version initiale
--actuellement indiquée comme [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

#### Hint`[::<language>]`
Affirme qu’un `hint` donne le résultat attendu. Utile lorsqu’une le champ se remplit de manière dynamique en fonction d’une interpolation de variables.
Cette caractéristique est prévue pour une date ultérieure, après la version initiale
--actuellement indiquée comme [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).
-->

<!--
# Exécuter des tests
## Démarrage rapide pour exécuter un test sur un XML XForm
`xform-test path/to/file.xml`

## Démarrage rapide pour exécuter un test sur un XLSForm sur Excel
`xform-test path/to/file.[xls|xlsx]`
-->

<!--
# Interpréter les résultats d’un test
## Format des résultats de tests
Les résultats de tests sont renvoyés en JSON. La syntaxe est la suivante.
```
{
    "status": "[ PASS | FAIL ]",
    "exitCode": [ 0 | 1 ],
    "statusCode": [0 | 1.x],
    "details": {
        "nodeId": !String
        "label": !String,
        "pointOfFailure": !String,
        "failedAssertionExpression": !String
    },
    "message": !String
}
```

Voici les détails des différents champs du résultat de test.

- `status`: Toujours présent. Caractères. Sera `"PASS"` ou `"FAIL"`.
- `exitCode`: Toujours présent. Numérique. Sera `0` si le test réussi, ou `1` s’il échoue.
- `statusCode`: Toujours présent. Numérique. Sera `0` si le test réussi, ou `1.x` s’il échoue, `x` étant une valeur numérique représentant le code numérique spécifique du cas d’échec.
- `details`: Toujours présent. Object ou null. Détaille où et pourquoi le test a échoué.
    - `nodeId`: Toujours présent si échec. Caractères. Représente le nom de node de XForm dans `<instance>`. Dans un
    XLSForm, c’est le champ `name` d’une rangée donnée.
    - `label`: Toujours présent si échec. String ou null. La langue utilisée pour le label sera la langue par défaut du formulaire, si précisée.
    - `pointOfFailure`: Toujours présent si échec. String. Sera soit (a) le nom de l’attribut du node contenant
    l’expression d’échec, ex : `relevant`, `constraint`, `choice_filter`, soit (b) `value` si une valeur saisie a été affirmée,
    soit (c) `choice list` si une liste de choix disponible a été affirmée –selon l’option qui s’applique.
    - `failedAssertionExpression`: Toujours présent si échec. Caractères. Sera soit (a) l’expression littérale de l’attribut d’échec, (b) une saisie de valeur affirmée, soit (c) une liste de choix disponible affirmée –selon l’option qui s’applique.
- `message`: Toujours présent. Caractères. Un message décrivant en une ou plusieurs phrases ce qu’il s’est passé, où, et apportant parfois plus d’informations.


## Exemple d’un test réussi
```json
{
    "status": "PASS",
    "exitCode": 0,
    "statusCode": 0,
    "details": null,
    "message": "Success"
}
```

## Interpréter les résultats des tests qui ont échoué
Cette section détaille tous les cas d’échec de test, et comment les interpréter. Le numéro devant chaque sous-titre suivant représente le
`statusCode` désigné pour un cas d’échec en particulier. Pour chaque cas expliqué, un exemple de l’assertion de test qui a été faite est fourni, ainsi qu’un exemple de message/texte du résultat de test qui s’afficherait après avoir échoué.

### 1.1 Quand une `value` simulée ne peut être insérée en raison de `constraint`
|Type d’assertion| Exemple de XLSForm |Exemple de XForm
|--------------|-----------------|-------------
|valeur         |15               |<bind ... xform-test-linearAssert="15"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.1,
    "details": {
        "nodeId": "age",
        "label": "Enter the respondent's age.",
        "pointOfFailure": "constraint",
        "failedAssertionExpression": ". > 18"
    },
    "message": "Le test de scénario linéaire a échoué sur le node 'âge'. L’assertion de la valeur '15' a été créée, mais cela enfreint la contrainte '. > 18' "
}
```

### 1.2 Quand une `value` simulée ne peut être insérée en raison de `relevant`
|Type d’assertion | Exemple de XLSForm |Exemple de XForm
|--------------|-----------------|-------------
|valeur         |oui              |<bind ... xform-test-linearAssert="yes"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.2,
    "details": {
        "nodeId": "abortion_attempt_yn",
        "label": "Avez-vous déjà fait quelque chose pour interrompre une grossesse ou faire revenir vos règles ?",
        "pointOfFailure": "relevant",
        "failedAssertionExpression": "${has_been_pregnant} = 'yes'"
    },
    "message": "Le test du scénario linéaire a échoué sur le node 'abortion_attempt_yn'. Une assertion de valeur a été faite, mais la valeur n’a pu être saisie car 'relevant' a estimé 'false' l’expression : ${has_been_pregnant} = 'oui''. Veuillez vérifier la logique et les assertions ayant mené à 'abortion_attempt_yn'.) "
}
```

### 1.3 Quand une `value` simulée ne peut être insérée en raison de `relevant` sur un node antérieur.
|Type d’assertion | Exemple de XLSForm |Exemple de XForm
|--------------|-----------------|-------------
|valeur         |Jane             |<bind ... xform-test-linearAssert="Jane"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.3,
    "details": {
        "nodeId": "recent_birth_name",
        "label": "Quel est le nom de votre dernier enfant (le plus récent) ?",
        "pointOfFailure": "relevant",
        "failedAssertionExpression": "${recent_birth_result} = 'live_birth'"
    },
    "message": "Le test de scénario linéaire a échoué sur le node 'recent_birth'. Une assertion de valeur a été faite, mais la valeur n’a pu être saisie car 'relevant' a estimé 'false' le node antérieur 'pregnancy_history_question_group'. L’expression 'relevant' du node antérieur était :  '${has_been_pregnant} = 'oui''. Veuillez vérifier la logique et les assertions qui ont mené à 'pregnancy_history_question_group'."
}
```

### 1.4 Quand une `value` simulée ne peut être insérée en raison de `choice_filter`
Cette caractéristique est prévue pour une date ultérieure, après la version initiale –actuellement indiquée comme [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

### 1.5 Échec d’une assertion `relevant`
|Type d’assertion | Exemple de XLSForm | Exemple de XForm
|--------------|-----------------|-------------
|relevant      |relevant: 0      |<bind ... xform-test-linearAssert="relevant: 0"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.5,
    "details": {
        "nodeId": "phone_number",
        "label": "What is your phone number?",
        "pointOfFailure": "relevant",
        "failedAssertionExpression": "${phone_number_permission} = 'yes'"
    },
    "message": "Le test de scénario linéaire a échoué sur le node 'phone_number'. Une assertion a été faite indiquant que 'relevant' devrait estimer 'false', mais a estimé 'true' l’expression : '${phone_number_permission} = 'oui''. Veuillez vérifier la logique et les assertions ayant mené à 'phone_number'."
}
```

### 1.6 Échec de l’assertion `constraint`
|Type d’assertion | Exemple de XLSForm |Exemple de XForm
|---	       |---	                   |---
|constraint    |value: 5, constraint: 0|<bind ... xform-test-linearAssert="value: 5, constraint: 0"/>
```json
{
    "status": "FAIL",
    "exitCode": 1,
    "statusCode": 1.6,
    "details": {
        "nodeId": "days_per_week_open",
        "label": "How many days a week is this facility open?",
        "pointOfFailure": "constraint",
        "failedAssertionExpression": ". >= 7"
    },
    "message": "Le test de scénario linéaire a échoué sur le node 'days_per_week_open'. Une assertion a été créée indiquant que 'constraint' devrait évaluer 'false' ce qui signifie que la valeur saisie dans l’assertion devrait avoir été valide. Cependant, la contrainte a évalué 'true' l’expression : '. >= 7' pour la valeur '5'."
}
```

Dans l’exemple ci-dessus, nous pouvons voir que la cause de l’échec était un échange accidentel des signes « inférieur à » et « supérieur à ».

### 1.7 Échec de l’assertion `choice_filter`
Cette caractéristique est prévue pour une date ultérieure, après version initiale –actuellement indiquée comme [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

### 1.8 Échec de l’assertion de la liste de `choices`
Cette caractéristique est prévue pour une date ultérieure, après version initiale –actuellement indiquée comme [release 1.1](
https://github.com/PMA-2020/xform-test/releases/tag/1.1.0).

# Référence CLI
## Syntaxe
`xform-test path/to/file [options]`

## Options
| Alerte courte | Alerte longue | Description
|------------|-----------|------------
| -h         | --help    |Accéder au fichier d’aide.
| -o         | --output  |Désigner un répertoire d’outputs spécifiques. Devrait être sous la forme `-o path/to/dir`.
-->

# Autres
#### Ressources utiles
**Ressources générales**
- [Documentation sur XForm Test](http://xform-test.pma2020.org)
- [XForm Test sur GitHub](https://github.com/PMA-2020/xform-test/)
- [Open Data Kit](https://opendatakit.org/) – Une plateforme de collecte de données en accès libre utilisant XForms.
- [PMA](https://www.pmadata.org/) – Une organisation programmatique de collecte de données pour des enquêtes permettant de générer rapidement des résultats afin de surveiller des indicateurs clés de santé et de développement. Lieu de naissance de  _XForm Test_.
- [PMA sur GitHub](https://www.github.com/PMA-2020) – Projets et initiatives de PMA en accès libre.

**Autres projets de tests basés sur XML et XForm **
- [XMLUnit](https://github.com/xmlunit/xmlunit) - Langue native XML, plateforme et répertoire agnostiques permettant la comparaison des documents XML, validation de schémas, et assertion de valeur XPath
- [XForms Unit](http://eric.van-der-vlist.com/blog/2013/08/20/xforms-unit/) – Un répertoire de tests unitaires non appliqués
- [Orbeon Testsuite](https://github.com/nvdbleek/com.orbeon.testsuite.w3c) – Permet d’exécuter les tests basés sur selenium spécifiquement pour Orbeon
- [XForms Client-Side Unit Tests](http://wiki.orbeon.com/forms/doc/contributor-guide/xforms-unit-tests) – Une autre suite de tests propre à
Orbeon pour les tests unitaires exécutés sous JavaScript

#### Rapporter des anomalies
Envoyez un email à jflack@jhu.edu, ou mieux encore, [postez votre problème sur GitHub](
https://github.com/PMA-2020/xform-test/issues/new).

#### Collaborer
[XForm-test](https://github.com/PMA-2020/xform-test) peut être « forké » sur GitHub. Les « Pull requests » sont bienvenues !

#### Obtenir de l’aide
Envoyez un email à jflack@jhu.edu ou [postez votre problème sur GitHub](https://github.com/PMA-2020/xform-test/issues/new). Pour le moment, vous pouvez obtenir de l’aide en temps réel sur Skype. Envoyez un message à : `joeflack4`.

