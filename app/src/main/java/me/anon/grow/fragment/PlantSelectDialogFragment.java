package me.anon.grow.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import lombok.Setter;
import me.anon.controller.adapter.PlantSelectionAdapter;
import me.anon.grow.R;
import me.anon.lib.Views;
import me.anon.lib.manager.PlantManager;
import me.anon.model.Plant;
import me.anon.view.PlantSelectHolder;

/**
 * // TODO: Add class description
 *
 * @author 7LPdWcaW
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
@Views.Injectable
public class PlantSelectDialogFragment extends DialogFragment
{
	public interface OnDialogActionListener
	{
		public void onDialogAccept(int plantIndex, boolean showImage);
	}

	private PlantSelectionAdapter adapter;
	@Views.InjectView(R.id.recycler_view) private RecyclerView recyclerView;
	private boolean showImages = true;
	@Setter private OnDialogActionListener onDialogActionListener;

	@SuppressLint("ValidFragment")
	public PlantSelectDialogFragment()
	{
	}

	@Override public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		View view = getActivity().getLayoutInflater().inflate(R.layout.plant_list_dialog_view, null, false);
		Views.inject(this, view);

		adapter = new PlantSelectionAdapter(PlantManager.getInstance().getSortedPlantList(null), null, getActivity())
		{
			@Override public void onBindViewHolder(PlantSelectHolder holder, int position)
			{
				super.onBindViewHolder(holder, position);

				if (!showImages)
				{
					ImageLoader.getInstance().cancelDisplayTask(holder.getImage());
					holder.getImage().setImageDrawable(null);
				}

				final Plant plant = getPlants().get(position);
				holder.itemView.setOnClickListener(new View.OnClickListener()
				{
					@Override public void onClick(View view)
					{
						boolean check = !((CheckBox)view.findViewById(R.id.checkbox)).isChecked();
						((CheckBox)view.findViewById(R.id.checkbox)).setChecked(check);

						if (check)
						{
							getSelectedIds().clear();
							getSelectedIds().add(plant.getId());
						}
						else
						{
							getSelectedIds().remove(plant.getId());
						}

						adapter.notifyDataSetChanged();
					}
				});
			}
		};

		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

		final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
			.setTitle("Select plant")
			.setView(view)
			.setPositiveButton("Ok", null)
			.setNeutralButton("Hide images", null)
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					dialog.dismiss();
				}
			}).create();

		alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override public void onShow(DialogInterface dialogInterface)
			{
				alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override public void onClick(View view)
					{
						if (onDialogActionListener != null)
						{
							if (adapter.getSelectedIds().size() == 0) return;

							int plantIndex = -1;
							for (int index = 0; index < PlantManager.getInstance().getPlants().size(); index++)
							{
								if (PlantManager.getInstance().getPlants().get(index).getId().equalsIgnoreCase(adapter.getSelectedIds().get(0)))
								{
									plantIndex = index;
									break;
								}
							}

							if (plantIndex > -1)
							{
								onDialogActionListener.onDialogAccept(plantIndex, showImages);
							}
						}

						alertDialog.dismiss();
					}
				});

				alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
				{
					@Override public void onClick(View v)
					{
						getActivity().finish();
					}
				});

				alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
				{
					@Override public void onClick(View view)
					{
						showImages = !showImages;
						adapter.notifyDataSetChanged();

						if (showImages)
						{
							((TextView)view).setText("Hide images");
						}
						else
						{
							((TextView)view).setText("Show images");
						}
					}
				});
			}
		});

		return alertDialog;
	}
}
