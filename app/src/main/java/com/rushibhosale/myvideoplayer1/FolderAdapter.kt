package com.rushibhosale.myvideoplayer1
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rushibhosale.myvideoplayer1.databinding.FolderItemViewBinding


class FolderAdapter(private val context:Context, private var folderList: ArrayList<Folder>):RecyclerView.Adapter<FolderAdapter.MyHolder>() {
    class MyHolder(binding:FolderItemViewBinding):RecyclerView.ViewHolder(binding.root){
        var title = binding.FolderName
        var root = binding.root
        val checkbox =binding.checkbox1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(FolderItemViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val isSelectOn = MainActivity.isSelectON
        val mainBinding = MainActivity.binding
        val folder = folderList[position]
        holder.checkbox.visibility = if (isSelectOn) View.VISIBLE else View.GONE
        holder.checkbox.setImageResource(if(MainActivity.selectAll)R.drawable.checked_box else R.drawable.empty_check_box)
        holder.title.text = folderList[position].folderName
        holder.root.setOnClickListener{
            if(!MainActivity.isSelectON){
                val intent = Intent(context, FoldersActivity::class.java)
                intent.putExtra("position", position)
                context.startActivity(intent)

            }else{
                if(folder in MainActivity.selectedFolderList){
                    MainActivity.selectedFolderList.remove(folder)
                    MainActivity.selected -=1
                    mainBinding.selected.text = "${MainActivity.selected}"
                    holder.checkbox.setImageResource(R.drawable.empty_check_box)
                }
                else{
                    MainActivity.selectedFolderList.add(folder)
                    MainActivity.selected +=1
                    mainBinding.selected.text = "${MainActivity.selected}"
                    holder.checkbox.setImageResource(R.drawable.checked_box)
                }
            }

        }

    }

    override fun getItemCount(): Int {
        return folderList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    public fun updateList(updatedList:ArrayList<Folder>){
        folderList = updatedList
        notifyDataSetChanged()
    }
}