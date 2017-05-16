package dakara.eclipse.plugin.kavi.picklist;

import java.util.LinkedHashMap;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public final class KaviListItem<T> {
	public final T dataItem;
	private LinkedHashMap<Integer, Score> scores = new LinkedHashMap<>();
	private boolean allColumnsRequired = false;
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
	
	public void setScoreModeAllRequired(boolean allColumnsRequired) {
		this.allColumnsRequired = allColumnsRequired;
	}
	
	public int totalScore() {
		if (allColumnsRequired)
			if (scores.values().stream().mapToInt(score -> score.rank).anyMatch(rank -> rank < 0)) return 0;
		return scores.values().stream().mapToInt(score -> score.rank).sum();
	}
}