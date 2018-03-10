package dakara.eclipse.plugin.platform;

public class ResourceItem {
	public final String name;
	public final String path;
	public final String project;
	public ResourceItem(String name, String path, String project) {
		if (name == null) name = "";
		if (path == null) path = "";
		if (project == null) project = "";
		
		this.name = name;
		this.path = path;
		this.project = project;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof ResourceItem)) return false;
		ResourceItem other = (ResourceItem) obj;
		
		if (name.equals(other.name) && path.equals(other.path) && project.equals(other.project)) return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() ^ path.hashCode() ^ project.hashCode();
	}
}
