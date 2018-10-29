"""Tests for Xform Test Suite"""
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
            version = search('[0-9]\.[0-9]\.[0-9]', file)[0]
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
        # With sans_temp_files, you can have Excel files open while testing.
        sans_temp_files = [x for x in all_files
                           if not x[len(self.input_path()):].startswith('~$')]
        return sans_temp_files

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
            1. str: TODO
            2. str: TODO
        """
        in_files = self.input_files()

        print(COMMAND_HEAD)
        command = \
            command_base_for_latest_jar(LIBS_DIR) + in_files + options
        subprocess.call(command)

        # TODO
        expected = True
        actual = False
        return expected, actual

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
        options_list = CliTest._dict_options_to_list(options)

        expected, actual = self.standard_cli(options_list)
        self.assertEqual(expected, actual)


class MultipleFiles(CliTest):
    """Can run CLI on multiple files at once?"""

    def test_cli(self):
        """Simple smoke test to see that CLI runs without error."""
        self.standard_cli_test()


if __name__ == '__main__':
    unittest.main()
