package dakara.eclipse.plugin.platform;

import java.io.IOException;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.index.Index;
import org.eclipse.jdt.internal.core.index.IndexLocation;
import org.eclipse.jdt.internal.core.search.BasicSearchEngine;
import org.eclipse.jdt.internal.core.search.PatternSearchJob;
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
	
	public static List<ResourceItem>	collectAllWorkspaceTypes() {
		List<ResourceItem> files = new ArrayList<>();
		IJavaSearchScope scope = BasicSearchEngine.createWorkspaceScope();
		PatternSearchJob job = new PatternSearchJob(null, SearchEngine.getDefaultSearchParticipant(), scope, null);
		Index[] selectedIndexes = job.getIndexes(null);
		for (Index index : selectedIndexes) {
			IndexLocation indexLocation = index.getIndexLocation();
			try {
				String[] names = index.queryDocumentNames(null);
				if (names != null) {
					for (String name : names) {
						IPath filePath = Path.fromPortableString(name);
						String projectName =  Path.fromPortableString(index.containerPath).lastSegment();
						String fileName = filePath.lastSegment();
						String pathName = filePath.uptoSegment(filePath.segmentCount() - 1).toString();
						files.add(new ResourceItem(fileName, pathName, projectName));
					} 
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
