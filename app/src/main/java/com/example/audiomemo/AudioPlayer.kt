package com.example.audiomemo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import java.lang.Exception


class AudioPlayer {
    lateinit var audio: RecordingModel
    var title: TextView? = null
    private lateinit var btnplay: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var progressText: TextView
    lateinit var mediaPlayer: MediaPlayer
    var isPlaying = false
    private lateinit var runnable:Runnable
    private lateinit var handler: Handler

    constructor(audio: RecordingModel, btnplay: ImageView, seekBar: SeekBar, progressText: TextView, title: TextView?){
        this.audio = audio
        this.title = title
        this.btnplay = btnplay
        this.seekBar = seekBar
        this.progressText = progressText
//        this.mediaPlayer = mediaPlayer

        this.seekBar.visibility = View.GONE
        this.progressText.visibility = View.GONE

        handler = Handler(Looper.getMainLooper())

        this.title?.setText(audio.name)
        this.title?.setOnClickListener {
            var newFrag = RecordingViewFragment.newInstance(audio)
            (it.context as MainActivity).supportFragmentManager.beginTransaction().replace(R.id.container_fragm, newFrag).commit()
        }

        this.seekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        if (isPlaying){
    //                            mediaPlayer.seekTo(progress * 1000)
                        }
                    } else {
                        Log.e("Nprogress",progress.toString())
                    }
                }
    
                override fun onStartTrackingTouch(seek: SeekBar) {
                    // write custom code for progress is started
    
                }
    
                override fun onStopTrackingTouch(seek: SeekBar) {
                    // write custom code for progress is stopped
    //                    Toast.makeText(requireContext,"Progress is: " + seek.progress + "%", Toast.LENGTH_SHORT).show()
                    if (isPlaying){
                        mediaPlayer.seekTo(seek.progress * 1000)
                    }
                }
            }
        )
    }


    private fun initializeSeekBar() {
        this.seekBar.max = mediaPlayer.duration / 1000
        this.seekBar.progress = mediaPlayer.currentPosition / 1000

        this.progressText.text = "${this.seekBar.progress / 60}:${this.seekBar.progress % 60 / 10}${this.seekBar.progress % 10} " +
                "/ ${this.seekBar.max / 60}:${this.seekBar.max % 60 / 10}${this.seekBar.max % 10}"

        runnable = Runnable {
            if (isPlaying){
                this.seekBar.progress = mediaPlayer.currentPosition / 1000
                handler.postDelayed(runnable, 1000)
            } else {
                this.seekBar.progress = this.seekBar.max
                this.seekBar.progress = 0
            }

            this.progressText.text = "${this.seekBar.progress / 60}:${this.seekBar.progress % 60 / 10}${this.seekBar.progress % 10} " +
                    "/ ${this.seekBar.max / 60}:${this.seekBar.max % 60 / 10}${this.seekBar.max % 10}"
        }
        handler.postDelayed(runnable, 500)
    }


    fun stopAudio(){
        if (!mediaPlayer.isPlaying){
            mediaPlayer.stop()
            mediaPlayer.reset()
        }

        audio.name?.let { Log.e("my", it) }

        mediaPlayer.release()
        isPlaying = false
        this.seekBar.visibility = View.GONE
        this.progressText.visibility = View.GONE
        this.btnplay.setBackgroundResource(R.drawable.play_audio)
    }


    fun playAudio() {
        mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        try {
            mediaPlayer.setDataSource(btnplay.context, Uri.parse(this.audio.path))
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener(MediaPlayer.OnPreparedListener { mp ->
                mp.start()
                initializeSeekBar()
            }
            )
            mediaPlayer.setOnCompletionListener {
                stopAudio()
            }
            this.seekBar.visibility = View.VISIBLE
            this.progressText.visibility = View.VISIBLE
            isPlaying = true
            this.btnplay.setBackgroundResource(R.drawable.pause_playback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}