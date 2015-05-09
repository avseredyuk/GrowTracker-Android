package me.anon.controller.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import me.anon.grow.MainApplication;
import me.anon.grow.PlantDetailsActivity;
import me.anon.grow.R;
import me.anon.model.Plant;
import me.anon.view.ImageHolder;

/**
 * // TODO: Add class description
 *
 * @author 
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageHolder>
{
	@Getter @Setter private List<String> images = new ArrayList<>();

	@Override public ImageHolder onCreateViewHolder(ViewGroup viewGroup, int i)
	{
		return new ImageHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.image_item, viewGroup, false));
	}

	@Override public void onBindViewHolder(ImageHolder viewHolder, final int i)
	{
		final String imageUri = images.get(i);

		ImageLoader.getInstance().cancelDisplayTask(viewHolder.getImage());
		ImageLoader.getInstance().displayImage("file://" + imageUri, viewHolder.getImage(), MainApplication.getDisplayImageOptions());

		viewHolder.itemView.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View v)
			{
				Intent details = new Intent(v.getContext(), PlantDetailsActivity.class);
				details.putExtra("plant_index", i);
				v.getContext().startActivity(details);
			}
		});
	}

	@Override public int getItemCount()
	{
		return images.size();
	}
}
