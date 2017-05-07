package dakara.eclipse.plugin.kavi.picklist;

import java.util.ArrayList;
import java.util.List;

import dakara.eclipse.plugin.stringscore.StringScore.Score;

public final class KaviListItem<T> {
	public final T dataItem;
	private List<Score> scores = new ArrayList<>();
	public KaviListItem(T dataItem) {
		this.dataItem = dataItem;
	}
	public T getDataItem() {
		return dataItem;
	}
	public void addScore(Score score) {
		scores.add(score);
	}
	public Score getScore(int columnIndex) {
		return scores.get(columnIndex);
	}
	
	public int totalScore() {
		return scores.stream().mapToInt(score -> score.rank).sum();
	}
}