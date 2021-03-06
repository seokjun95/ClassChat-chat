package com.example.myapplication.navigation.board

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MyGlobals
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.fragment_board.*
import kotlinx.android.synthetic.main.fragment_board.view.*
import kotlinx.android.synthetic.main.item_class.view.*


class BoardFragment : Fragment() {
    var user : FirebaseUser? = null
    var firestore : FirebaseFirestore? = null
    var classSnapshot: ListenerRegistration? = null
    lateinit var classListAdapter : ClassListRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_board, container, false)
        classSnapshot = null
        user = FirebaseAuth.getInstance().currentUser
        firestore = FirebaseFirestore.getInstance()
        Log.i("oncreateView","oo")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("onViewCreated","ov")
        classListAdapter = ClassListRecyclerViewAdapter()
        view.boardFragment_recyclerview.adapter = classListAdapter
        view.boardFragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        Log.i("onViewCreated","done")
    }
    override fun onStop() {
        super.onStop()
        classSnapshot?.remove()
    }

    inner class ClassListRecyclerViewAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var classList : MutableList<ClassData> = mutableListOf<ClassData>()

        init{
            val classes = MyGlobals.getInstance().myClasses
            Log.i("init",classes.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                classes.forEach{
                    k,v -> classList.add(ClassData(k,v))
                }
            }
            if(classList.size == 0){
                boardFragment_recyclerview.visibility = View.GONE
                tv_no_class.visibility = View.VISIBLE
            }
            else{
                boardFragment_recyclerview.visibility = View.VISIBLE
                tv_no_class.visibility = View.GONE
            }
            notifyDataSetChanged()
            """
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("test ", uid.toString())
            
            classSnapshot = firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                classList.clear()
                Log.d("query",querySnapshot.toString())
                if(querySnapshot == null)return@addSnapshotListener
                val userInfo = querySnapshot.toObject(userDTO::class.java!!)
                Log.d("userinfo",userInfo.toString())
                if(userInfo == null)return@addSnapshotListener
                val classesInfo = userInfo!!.classes
                for (classKey in classesInfo.keys){
                    classList.add(ClassData(classKey,classesInfo.get(classKey)!!))
                }
                if(classList.size == 0){
                    boardFragment_recyclerview.visibility = View.GONE
                    tv_no_class.visibility = View.VISIBLE
                }
                else{
                    boardFragment_recyclerview.visibility = View.VISIBLE
                    tv_no_class.visibility = View.GONE
                }
                
            }
            """

"""
            classList.add(ClassData("GED-1234", "종합설계프로젝트"))
            classList.add(ClassData("EDD-202", "과학영어"))
            classList.add(ClassData("SAM-572", "네트워크개론"))
            classList.add(ClassData("SAS-611", "언어논리입문"))
            classList.add(ClassData("SWE-1552", "핵심취업전략"))
            notifyDataSetChanged()
   """
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class,parent,false)
            return CustumViewHolder(view)
        }

        override fun getItemCount(): Int {
            return classList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.tv_class_name.text = classList[position].className
            holder.itemView.setOnClickListener {v->
                var intent = Intent(v.context,BoardInsideActivity::class.java)
                intent.putExtra("className",classList[position].className)
                intent.putExtra("classId",classList[position].classId)
                v.context.startActivity(intent)
            }

        }
        inner class CustumViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
}