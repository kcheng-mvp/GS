import java.awt.datatransfer.StringSelection
import java.awt.Toolkit

def copy(msg) {
    def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
    clipboard.setContents(new StringSelection(msg), null)
}
