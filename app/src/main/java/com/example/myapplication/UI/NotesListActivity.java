package com.example.myapplication.UI;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.Data.NoteRepository;
import com.example.myapplication.Model.Note;
import com.example.myapplication.R;
import java.util.List;

public class NotesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NoteRepository repository;
    private EditText etSearch;
    private NotesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        etSearch = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.recyclerNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        repository = new NoteRepository(this);
        loadNotes("");

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadNotes(String query) {
        List<Note> notes = repository.search(query);
        adapter = new NotesAdapter(notes);
        recyclerView.setAdapter(adapter);
    }

    class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {
        private List<Note> notes;

        public NotesAdapter(List<Note> notes) {
            this.notes = notes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Note note = notes.get(position);
            holder.tvSummary.setText(note.summary);
            holder.tvKeywords.setText(note.keywords);
            
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(NotesListActivity.this, NoteDetailActivity.class);
                intent.putExtra("NOTE_ID", note.id);
                startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(NotesListActivity.this)
                        .setTitle("Supprimer la note")
                        .setMessage("Voulez-vous vraiment supprimer cette note ?")
                        .setPositiveButton("Supprimer", (dialog, which) -> {
                            repository.delete(note.id);
                            loadNotes(etSearch.getText().toString());
                            Toast.makeText(NotesListActivity.this, "Note supprimée", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return notes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSummary, tvKeywords;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSummary = itemView.findViewById(R.id.tvItemSummary);
                tvKeywords = itemView.findViewById(R.id.tvItemKeywords);
            }
        }
    }
}
