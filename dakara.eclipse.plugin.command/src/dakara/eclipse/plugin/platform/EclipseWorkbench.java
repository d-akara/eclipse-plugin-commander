package dakara.eclipse.plugin.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;


public class EclipseWorkbench {
	public static List<ResourceItem> collectAllWorkspaceFiles(IWorkspaceRoot workspace) {
		List<ResourceItem> files = new ArrayList<>();
		
		IResourceProxyVisitor visitor = new IResourceProxyVisitor() {
			public boolean visit(IResourceProxy proxy) throws CoreException {
				if (proxy.getType() != IResource.FILE) return true;
				if (proxy.isDerived()) return false;
				if (proxy.isPhantom()) return false;
				if (proxy.isHidden()) return false;
				IFile file = (IFile) proxy.requestResource();
				if (file.getProjectRelativePath().segment(0).equals("indices")) return false;
				files.add(makeResourceItem(file));
				return false;
			}
		};
		
		try {
			IResource[] resources = workspace.members();
			for(IResource resource : resources) {
				if (!resource.getProject().isOpen()) continue;
				resource.accept(visitor, 0);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		return files;
	}
	
	private static String makePathOnly(IPath path) {
		return path.removeLastSegments(1).toString();
	}	
	
	private static ResourceItem makeResourceItem(IFile file) {
		return new ResourceItem(file.getName(), makePathOnly(file.getProjectRelativePath()), file.getProject().getName());
	}
	
	public static void createListenerForEditorFocusChanges(IWorkbenchPage page, Consumer<ResourceItem> focusAction) {
		IPartListener2 pl = new IPartListener2() {
			public void partActivated(IWorkbenchPartReference ref) {
				if (!(ref instanceof IEditorReference)) return;
				IEditorReference editor = (IEditorReference) ref;
				try {
					IEditorInput editorInput = editor.getEditorInput();
					if (!(editorInput instanceof IFileEditorInput)) return;
					IFileEditorInput fileInput = (IFileEditorInput) editorInput;
					focusAction.accept(makeResourceItem(fileInput.getFile()));
				} catch (PartInitException e) {
					throw new RuntimeException(e);
				}
			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
			public void partClosed(IWorkbenchPartReference partRef) {	}
			public void partDeactivated(IWorkbenchPartReference partRef) {}
			public void partOpened(IWorkbenchPartReference partRef) {}
			public void partHidden(IWorkbenchPartReference partRef) {}
			public void partVisible(IWorkbenchPartReference partRef) {}
			public void partInputChanged(IWorkbenchPartReference partRef) {}
		};
		page.addPartListener(pl);
	}
}
