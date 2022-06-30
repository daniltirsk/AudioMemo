package com.example.audiomemo
import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.lang.Exception
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import java.net.URLEncoder




class AudioAdapter(val requireContext: Context, val audioList: MutableList<RecordingModel>) :
    RecyclerView.Adapter<AudioAdapter.ViewHolder>() {
    var mediaPlayer = MediaPlayer()
    var isPlaying = false
    private lateinit var runnable:Runnable
    private lateinit var handler: Handler
    private var curPlaying: AudioPlayer? = null
    private var colors = arrayOf(R.color.trivial,R.color.normal,R.color.important)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.tvTitle)
        val btnplay = itemView.findViewById<ImageView>(R.id.play_button)
        val seekBar = itemView.findViewById<SeekBar>(R.id.seekBar)
        val progressText = itemView.findViewById<TextView>(R.id.progress)
        lateinit var audioPlayer: AudioPlayer
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(requireContext).inflate(R.layout.audio_layout, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.audioPlayer = AudioPlayer(audioList[position],holder.btnplay, holder.seekBar, holder.progressText, holder.title)

        holder.itemView.setBackgroundColor(requireContext.getColor(colors[holder.audioPlayer.audio.importance!!]))


        holder.btnplay.setOnClickListener {
            if (curPlaying != null){
                if (curPlaying!!.isPlaying){
                    curPlaying!!.stopAudio()
                    if(curPlaying!!.audio.path != holder.audioPlayer.audio.path){
                        curPlaying = holder.audioPlayer
                        curPlaying!!.playAudio()
                    }
                } else {
                    holder.audioPlayer.playAudio()
                    curPlaying = holder.audioPlayer
                }
            } else {
                holder.audioPlayer.playAudio()
                curPlaying = holder.audioPlayer
            }
        }
    }

    override fun getItemCount(): Int{
        return audioList.size
    }
}
