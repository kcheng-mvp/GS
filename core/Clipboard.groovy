import java.awt.datatransfer.StringSelection
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

def copy(msg) {
    def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
    clipboard.setContents(new StringSelection(msg), null)
}

def paste(){
    def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
    clipboard.getContents().getTransferData(DataFlavor.stringFlavor);
}

