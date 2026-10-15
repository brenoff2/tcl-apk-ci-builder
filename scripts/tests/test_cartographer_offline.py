import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
SCRIPT=ROOT/'scripts/dev_context/cartographer_offline.sh'

class CartographerIsolationContract(unittest.TestCase):
    def test_offline_script_rejects_missing_arguments(self):
        result=subprocess.run(['bash',str(SCRIPT)],capture_output=True,text=True)
        self.assertNotEqual(0,result.returncode)
        self.assertIn('USAGE',result.stdout+result.stderr)

    def test_offline_script_refuses_output_inside_product_tree(self):
        with tempfile.TemporaryDirectory() as d:
            folder=Path(d)
            cart=folder/'cart';cart.mkdir()
            app=folder/'app';app.mkdir()
            result=subprocess.run(['bash',str(SCRIPT),str(cart),str(app),str(app/'.cartographer')],capture_output=True,text=True)
            self.assertNotEqual(0,result.returncode)
            self.assertIn('inside',result.stdout+result.stderr)

    def test_offline_script_does_not_start_without_bun(self):
        with tempfile.TemporaryDirectory() as d:
            folder=Path(d)
            cart=folder/'cart';cart.mkdir()
            app=folder/'app';app.mkdir()
            result=subprocess.run(['bash',str(SCRIPT),str(cart),str(app),str(folder/'out')],env={'PATH':'/usr/bin:/bin'},capture_output=True,text=True)
            self.assertNotEqual(0,result.returncode)
            self.assertIn('bun',result.stdout+result.stderr)

if __name__=='__main__': unittest.main()
