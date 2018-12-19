"""Tests for Xform Test Suite"""
import ntpath
import os
import subprocess
import unittest
from distutils.version import LooseVersion
from glob import glob
from re import search

TEST_DIR = os.path.dirname(os.path.realpath(__file__)) + '/'
TEST_STATIC_DIR = TEST_DIR + 'static/'
PROJECT_ROOT_DIR = TEST_DIR + '../'
LIBS_DIR = PROJECT_ROOT_DIR + 'build/libs/'
COMMAND_HEAD = ['java', '-jar', LIBS_DIR + 'xform-test-0.2.0.jar']


def command_base_for_latest_jar(directory):
    """Get command list for highest versioned jar w/out options in dir.

    Args:
        directory (str): Path to directory containing jar files with semvar
        version named files, e.g. `my-package-x.y.z.jar`.

    Returns:
        list: Command list for jar.
    """
    return ['java', '-jar', latest_jar(directory)]


def latest_jar(directory):
    """Gets name of highest versioned jar in directory.

    Args:
        directory (str): Path to directory containing jar files with semvar
        version named files, e.g. `my-package-x.y.z.jar`.

    Returns:
        str: Name of jar file.
    """

    files = glob(directory + '*.jar')
    path_version_map = {}

    if len(files) < 1:
        return ''
    elif len(files) == 1:
        return files[0]
    else:
        latest_version_num = ''
        for file in files:
            version = search(r'[0-9]\.[0-9]\.[0-9]', file)[0]
            path_version_map[version] = file
        for k, v in path_version_map.items():
            if latest_version_num == '':
                latest_version_num = k
            else:
                if LooseVersion(k) > LooseVersion(latest_version_num):
                    latest_version_num = k
        latest_version_file_path = path_version_map[latest_version_num]
        return latest_version_file_path


class CliTest(unittest.TestCase):
    """Base class for running simple CLI tests."""

    @classmethod
    def files_dir(cls):
        """Return name of test class."""
        return TEST_STATIC_DIR + cls.__name__

    def input_path(self):
        """Return path of input file folder for test class."""
        return self.files_dir() + '/input/'

    def input_files(self):
        """Return paths of input files for test class."""
        all_files = glob(self.input_path() + '*')
        # With "sans_temps", you can have Excel files open while testing.
        sans_temps_and_dirs = [x for x in all_files if
                               not x[len(self.input_path()):].startswith('~$')
                               and not os.path.isdir(x)]
        return sans_temps_and_dirs

    @staticmethod
    def _dict_options_to_list(options):
        """Converts a dictionary of options to a list.

        Args:
            options (dict): Options in dictionary form, e.g. {
                'OPTION_NAME': 'VALUE',
                'OPTION_2_NAME': ...
            }

        Returns:
            list: A single list of strings of all options of the form
            ['--OPTION_NAME', 'VALUE', '--OPTION_NAME', ...]

        """
        new_options = []

        for k, v in options.items():
            new_options += ['--'+k, v]

        return new_options

    def standard_cli(self, options=[]):
        """Runs CLI.

        Args:
            options (list): A single list of strings of all options of the form
            ['--OPTION_NAME', 'VALUE', '--OPTION_NAME', ...]

        Returns:
            1. str: Expected error message (empty string).
            2. str: Actual error message, if any.
        """
        msg = 'TestFailure: Error occurred while running test.\n\n' \
              'Details:\n{}'
        expected_err = ''
        in_files = self.input_files()
        command = command_base_for_latest_jar(LIBS_DIR) + in_files + options

        try:
            process = subprocess.Popen(command,
                                       stdout=subprocess.PIPE,
                                       stderr=subprocess.PIPE)
            process.wait()
            err_msg = process.stderr.read().decode().strip()
            # TODO: Temp
            stdout = process.stdout.read().decode().strip()
            print(stdout)
            process.stderr.close()
            process.stdout.close()
        except Exception as err_msg:
            err_msg = str(err_msg)
        if err_msg:
            self.assertEqual(expected_err, err_msg, msg.format(err_msg))

        return expected_err, err_msg

    def standard_cli_test(self, options={}):
        """Checks CLI success.

        Args:
            options (dict): Options in dictionary form, e.g. {
                'OPTION_NAME': 'VALUE',
                'OPTION_2_NAME': ...
            }

        Side effects:
            assertEqual()
        """
        for file in self.input_files():  # temp until xform-test doesn't make
            if file.endswith('-modified.xml'):
                os.remove(file)

        options_list = CliTest._dict_options_to_list(options)

        expected, actual = self.standard_cli(options_list)
        self.assertEqual(expected, actual)

        for file in self.input_files():  # temp until xform-test doesn't make
            if file.endswith('-modified.xml'):
                os.remove(file)


class ConvertFirstTest(CliTest):
    """Augments CliTest with methods for handling XForms and XLSForms."""

    @staticmethod
    def input_src_files(path):
        """Return paths of input files for test class.

        Args:
            path (str): Path to dir with input files.

        Returns:
            list: Of files.
        """
        all_files = glob(path + '*')
        # With "sans_temps", you can have Excel files open while testing.
        sans_temps_and_dirs = [x for x in all_files if
                               not x[len(path):].startswith('~$')
                               and not os.path.isdir(x)]
        return sans_temps_and_dirs

    @staticmethod
    def delete_if_bad_extension(files, ok_extensions):
        """Remove files with bad extension

        Args:
            files (list): List of paths of files.
            ok_extensions (list): List of extensions that are ok to keep.

        Side Effects:
            Removes non-xml files from file system, not simply from an
            in-memory array of references to test files.
        """
        for file in files:
            if not any([file.endswith(x) for x in ok_extensions]):
                os.remove(file)

    @staticmethod
    def convert_command(in_file, out_file):
        """This isn't actually implemented because this is an abstract base.

        This will actually be overridden in child class.
        """
        return []

    def update_xml_files(self):
        """Update XML files."""
        path = self.input_path() + 'src/'
        with open(os.devnull, 'w') as null:
            for in_file in self.input_src_files(path):
                in_filename = ntpath.basename(in_file)
                out_filename = in_filename.replace('.xlsx', '.xml')
                out_file = in_file.replace(in_filename, '../' + out_filename)
                command = self.convert_command(in_file, out_file)
                process = subprocess.Popen(command, stdout=null)
                process.wait()
        null.close()

    def setUp(self):
        """setUp"""
        self.update_xml_files()
        StandardXLSFormTest.delete_if_bad_extension(files=self.input_files(),
                                                    ok_extensions=['.xml'])


class StandardXLSFormTest(ConvertFirstTest):
    """Tests for standard XLSForm spec files, as for most ODK forms."""

    @staticmethod
    def convert_command(in_file: '', out_file: '') -> []:
        """Convert xls/xlsx to xml."""
        return ['xls2xform', in_file, out_file]


class JHUCollectTest(ConvertFirstTest):
    """Tests for JHU Collect."""

    @staticmethod
    def convert_command(in_file: '', out_file: '') -> []:
        """Convert xls/xlsx to xml."""
        # 	python2 -m qtools2.convert -r test/static/CRVS/input/src/ET*.xlsx
        # 	mv test/static/CRVS/input/src/*.xml test/static/CRVS/input
        filename_after_convert = in_file.replace('.xlsx', '.xml')\
            .replace('.xls', '.xml')
        command = ['python2', '-m', 'qtools2.convert', '-r', in_file, ';'] + \
                  ['mv', filename_after_convert, out_file]
        return command


class MultipleFiles(StandardXLSFormTest):
    """Can run CLI on multiple files at once?"""

    def test_cli(self):
        """Simple smoke test to see that CLI runs without error."""
        super(StandardXLSFormTest, self).setUp()
        self.standard_cli_test()


class MultipleTestCases(StandardXLSFormTest):
    """Can run CLI on multiple files at once?"""

    def test_cli(self):
        """Simple smoke test to see that CLI runs without error."""
        self.standard_cli_test()


class CRVS(JHUCollectTest):
    """Would XFormTest actually work on a real scenario like PMA2020 CRVS?"""

    def test_cli(self):
        """Simple smoke test to see that CLI runs without error."""
        super(JHUCollectTest, self).setUp()
        self.standard_cli_test()


# TODO
# class ValueAssertionError(XFormsTest):
#     """Can run CLI on multiple files at once?"""
#
#     def test_cli(self):
#         """Simple smoke test to see that CLI runs without error."""
#         self.standard_cli_test()


if __name__ == '__main__':
    unittest.main()
