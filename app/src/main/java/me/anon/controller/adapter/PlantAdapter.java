package me.anon.controller.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import me.anon.grow.MainApplication;
import me.anon.grow.PlantDetailsActivity;
import me.anon.grow.R;
import me.anon.lib.Unit;
import me.anon.lib.manager.PlantManager;
import me.anon.model.Plant;
import me.anon.view.PlantHolder;

/**
 * // TODO: Add class description
 *
 * @author 7LPdWcaW
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
public class PlantAdapter extends RecyclerView.Adapter<PlantHolder> implements ItemTouchHelperAdapter
{
	@Getter private List<Plant> plants = new ArrayList<>();
	private Context context;
	@Getter private Unit measureUnit, deliveryUnit;

	public PlantAdapter(Context context)
	{
		this.context = context;

		measureUnit = Unit.getSelectedMeasurementUnit(context);
		deliveryUnit = Unit.getSelectedDeliveryUnit(context);
	}

	public void setPlants(List<Plant> plants)
	{
		this.plants.clear();
		this.plants.addAll(plants);
		this.plants.removeAll(Collections.singleton(null));
	}

	@Override public PlantHolder onCreateViewHolder(ViewGroup viewGroup, int i)
	{
		return new PlantHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.plant_item, viewGroup, false));
	}

	@Override public void onBindViewHolder(PlantHolder viewHolder, final int i)
	{
		final Plant plant = plants.get(i);
		viewHolder.getName().setText(plant.getName());

		String summary = plant.generateLongSummary(viewHolder.itemView.getContext());
		viewHolder.getSummary().setText(Html.fromHtml(summary));

		ImageLoader.getInstance().cancelDisplayTask(viewHolder.getImage());
		if (plant.getImages() != null && plant.getImages().size() > 0)
		{
			ImageAware imageAware = new ImageViewAware(viewHolder.getImage(), true);
			ImageLoader.getInstance().displayImage("file://" + plant.getImages().get(plant.getImages().size() - 1), imageAware, MainApplication.getDisplayImageOptions());
		}
		else
		{
			viewHolder.getImage().setImageResource(R.drawable.default_plant);
		}

		viewHolder.itemView.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v)
			{
				Intent details = new Intent(v.getContext(), PlantDetailsActivity.class);
				details.putExtra("plant_index", PlantManager.getInstance().getPlants().indexOf(plant));
				v.getContext().startActivity(details);
			}
		});
	}

	@Override public int getItemCount()
	{
		return plants.size();
	}

	@Override public void onItemMove(int fromPosition, int toPosition)
	{
		if (fromPosition < toPosition)
		{
			for (int index = fromPosition; index < toPosition; index++)
			{
				Collections.swap(plants, index, index + 1);
			}
		}
		else
		{
			for (int index = fromPosition; index > toPosition; index--)
			{
				Collections.swap(plants, index, index - 1);
			}
		}

		notifyItemMoved(fromPosition, toPosition);
	}

	@Override public void onItemDismiss(int position)
	{
		plants.remove(position);
		notifyItemRemoved(position);
	}
}
