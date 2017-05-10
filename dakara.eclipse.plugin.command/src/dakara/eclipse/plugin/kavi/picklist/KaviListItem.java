package dakara.eclipse.plugin.kavi.picklist;

import java.util.LinkedHashMap;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public final class KaviListItem<T> {
	public final T dataItem;
	private LinkedHashMap<Integer, Score> scores = new LinkedHashMap<>();
	public KaviListItem(T dataItem) {
		this.dataItem = dataItem;
	}
	public T getDataItem() {
		return dataItem;
	}
	public void addScore(Score score, int columnIndex) {
		scores.put(columnIndex, score);
	}
	public Score getColumnScore(int columnIndex) {
		return scores.get(columnIndex);
	}
	
	public int totalScore() {
		return scores.values().stream().mapToInt(score -> score.rank).sum();
	}
}