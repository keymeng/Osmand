package net.osmand.plus.wikivoyage.explore.travelcards;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;

import java.util.List;

public class TravelNeededMapsCard extends BaseTravelCard {

	public static final int TYPE = 70;

	private DownloadIndexesThread downloadThread;
	private List<IndexItem> items;

	private Drawable downloadIcon;
	private Drawable cancelIcon;

	private CardListener listener;
	private View.OnClickListener onItemClickListener;

	public void setListener(CardListener listener) {
		this.listener = listener;
	}

	public TravelNeededMapsCard(OsmandApplication app, boolean nightMode, List<IndexItem> items) {
		super(app, nightMode);
		downloadThread = app.getDownloadThread();
		this.items = items;
		downloadIcon = getActiveIcon(R.drawable.ic_action_import);
		cancelIcon = getActiveIcon(R.drawable.ic_action_remove_dark);
		onItemClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onIndexItemClick((IndexItem) view.getTag());
				}
			}
		};
	}

	@Override
	public void bindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
		if (viewHolder instanceof NeededMapsVH) {
			NeededMapsVH holder = (NeededMapsVH) viewHolder;

			holder.description.setText(isInternetAvailable()
					? R.string.maps_you_need_descr : R.string.no_index_file_to_download);
			adjustChildCount(holder.itemsContainer);

			boolean paidVersion = Version.isPaidVersion(app);

			for (int i = 0; i < items.size(); i++) {
				IndexItem item = items.get(i);
				boolean downloading = downloadThread.isDownloading(item);
				boolean currentDownloading = downloading && downloadThread.getCurrentDownloadingItem() == item;
				boolean lastItem = i == items.size() - 1;
				View view = holder.itemsContainer.getChildAt(i);

				if (item.isDownloaded()) {
					view.setOnClickListener(null);
				} else {
					view.setTag(item);
					view.setOnClickListener(onItemClickListener);
				}

				((ImageView) view.findViewById(R.id.icon))
						.setImageDrawable(getActiveIcon(item.getType().getIconResource()));
				((TextView) view.findViewById(R.id.title))
						.setText(item.getVisibleName(app, app.getRegions(), false));
				((TextView) view.findViewById(R.id.description)).setText(getItemDescription(item));

				ImageView iconAction = (ImageView) view.findViewById(R.id.icon_action);
				Button buttonAction = (Button) view.findViewById(R.id.button_action);
				if (item.isDownloaded()) {
					iconAction.setVisibility(View.GONE);
					buttonAction.setVisibility(View.GONE);
				} else {
					boolean showBtn = !paidVersion && item.getType() == DownloadActivityType.WIKIPEDIA_FILE;
					iconAction.setVisibility(showBtn ? View.GONE : View.VISIBLE);
					buttonAction.setVisibility(showBtn ? View.VISIBLE : View.GONE);
					if (!showBtn) {
						iconAction.setImageDrawable(downloading ? cancelIcon : downloadIcon);
					}
				}

				ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
				progressBar.setVisibility(downloading ? View.VISIBLE : View.GONE);
				if (currentDownloading) {
					int progress = downloadThread.getCurrentDownloadingItemProgress();
					progressBar.setProgress(progress < 0 ? 0 : progress);
				} else {
					progressBar.setProgress(0);
				}

				view.findViewById(R.id.divider).setVisibility(lastItem ? View.GONE : View.VISIBLE);
			}

			boolean primaryBtnVisible = updatePrimaryButton(holder);
			boolean secondaryBtnVisible = updateSecondaryButton(holder);
			holder.buttonsDivider.setVisibility(primaryBtnVisible && secondaryBtnVisible ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public int getCardType() {
		return TYPE;
	}

	private void adjustChildCount(LinearLayout itemsContainer) {
		int itemsCount = items.size();
		int childCount = itemsContainer.getChildCount();
		if (itemsCount == childCount) {
			return;
		}
		if (itemsCount > childCount) {
			LayoutInflater inflater = LayoutInflater.from(itemsContainer.getContext());
			for (int i = childCount; i < itemsCount; i++) {
				inflater.inflate(R.layout.travel_needed_map_item, itemsContainer);
			}
		} else if (itemsCount < childCount) {
			itemsContainer.removeViews(0, childCount - itemsCount);
		}
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updateSecondaryButton(NeededMapsVH vh) {
		vh.secondaryBtnContainer.setVisibility(View.VISIBLE);
		vh.secondaryBtn.setText(isDownloading() ? R.string.shared_string_cancel : R.string.later);
		vh.secondaryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (listener != null) {
					listener.onSecondaryButtonClick();
				}
			}
		});
		return true;
	}

	/**
	 * @return true if button is visible, false otherwise.
	 */
	private boolean updatePrimaryButton(NeededMapsVH vh) {
		if (showPrimaryButton()) {
			boolean enabled = isInternetAvailable();
			vh.primaryBtnContainer.setVisibility(View.VISIBLE);
			vh.primaryBtnContainer.setBackgroundResource(getPrimaryBtnBgRes(enabled));
			vh.primaryButton.setTextColor(getResolvedColor(getPrimaryBtnTextColorRes(enabled)));
			vh.primaryButton.setEnabled(enabled);
			vh.primaryButton.setText(R.string.download_all);
			vh.primaryButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (listener != null) {
						listener.onPrimaryButtonClick();
					}
				}
			});
			return true;
		}
		vh.primaryBtnContainer.setVisibility(View.GONE);
		return false;
	}

	public boolean isDownloading() {
		for (IndexItem item : items) {
			if (downloadThread.isDownloading(item)) {
				return true;
			}
		}
		return false;
	}

	private boolean showPrimaryButton() {
		for (IndexItem item : items) {
			if (!item.isDownloaded() && !downloadThread.isDownloading(item)) {
				return true;
			}
		}
		return false;
	}

	private String getItemDescription(IndexItem item) {
		return app.getString(R.string.file_size_in_mb, item.getArchiveSizeMB()) + " • " + item.getType().getString(app);
	}

	public interface CardListener {

		void onPrimaryButtonClick();

		void onSecondaryButtonClick();

		void onIndexItemClick(IndexItem item);
	}

	public static class NeededMapsVH extends RecyclerView.ViewHolder {

		final TextView description;
		final LinearLayout itemsContainer;
		final View secondaryBtnContainer;
		final TextView secondaryBtn;
		final View buttonsDivider;
		final View primaryBtnContainer;
		final TextView primaryButton;

		@SuppressWarnings("RedundantCast")
		public NeededMapsVH(View itemView) {
			super(itemView);
			description = (TextView) itemView.findViewById(R.id.description);
			itemsContainer = (LinearLayout) itemView.findViewById(R.id.items_container);
			secondaryBtnContainer = itemView.findViewById(R.id.secondary_btn_container);
			secondaryBtn = (TextView) itemView.findViewById(R.id.secondary_button);
			buttonsDivider = itemView.findViewById(R.id.buttons_divider);
			primaryBtnContainer = itemView.findViewById(R.id.primary_btn_container);
			primaryButton = (TextView) itemView.findViewById(R.id.primary_button);
		}
	}
}