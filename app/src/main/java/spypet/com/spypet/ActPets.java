package spypet.com.spypet;

import android.*;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import controlador.GerenciadorSharedPreferences;
import controlador.Imagem;
import controlador.QRCode;
import controlador.Requisicao;
import controlador.TransformacaoCirculo;
import modelo.Animal;
import modelo.Compromisso;
import modelo.Especie;
import modelo.Evento;
import modelo.Mensagem;
import modelo.Raca;

/**
 * Created by Felipe on 24/08/2016.
 */
public class ActPets extends AppCompatActivity {

    public int tabSelecionada;
    private Animal animal;
    private Evento evento1;
    private ProgressDialog pd;
    private ArrayList<Especie> especies = new ArrayList<>();
    private ArrayList<Raca> racas = new ArrayList<>();
    private ArrayList<String> cores = new ArrayList<>();
    private ArrayList<String> idades = new ArrayList<>();
    private ArrayList<Evento> listaEventos = new ArrayList<>();
    private int processos = 0;
    private ImageView ivFotoAnimal;
    private EditText etNome;
    //private EditText etCor;
    //private EditText etIdade;
    private TextView tvNomePet;
    private TextView tvRacaPet;
    private ImageView ivRemover;
    private Spinner spEspecie;
    private Spinner spRaca;
    private Spinner spCor;
    private Spinner spIdade;
    private ArrayAdapter adEspecie;
    private ArrayAdapter adRaca;
    private ArrayAdapter adIdade;
    private ArrayAdapter adCor;
    private ImageView ivQRCode;
    private EditText etCaracteristicas;
    private CheckBox cbDesaparecido;
    private Button btSalvar;
    private Spinner spGenero;
    private Spinner spPorte;
    private AlertDialog.Builder dialogo;
    private AlertDialog alerta;
    private Intent selecionarImagem;
    Uri imagemSelecionada = null;
    private static final int READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 1;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST = 2;

    ListView lvEventos;
    ArrayAdapter<Evento> adpEventos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pets);

        //Configura e carrega toolbar
        Toolbar t = (Toolbar) findViewById(R.id.toolbar);
        t.setTitleTextColor(ContextCompat.getColor(this, R.color.fontColorPrimary));
        t.setLogo(R.drawable.ic_pata);
        setSupportActionBar(t);

        try{
            Intent intent = getIntent();
            JSONObject json = new JSONObject(intent.getStringExtra("Animal"));
            animal = Animal.jsonToAnimal(json);

            //JSONObject json1 = new JSONObject(intent.getStringExtra("Evento"));
            //evento1 = Evento.jsonToEvento(json1);

        }catch(Exception ex){
            Log.e("Erro", ex.getMessage());
            Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
        }

        pd = ProgressDialog.show(ActPets.this, "", "Por favor, aguarde...", false);
        processos++;

        carregaSpinners();

        recuperaDadosPet();

        listaCompromissos();

        carregaQRCode();

        configuraTabs();
    }

    //Verifica se o aplicativo tem permissão para acessar o armazenamento de arquivos
    @TargetApi(Build.VERSION_CODES.M)
    public void verificaPermissao(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //Verifica se o usuário selecionou a opções de não perguntar novamente.
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
            }else{
                alerta.show();
            }
        }else{
            //Abre tela para seleção da imagem
            startActivityForResult(selecionarImagem, 1);
        }
    }

    //Verifica se o aplicativo tem permissão para acessar o armazenamento de arquivos
    @TargetApi(Build.VERSION_CODES.M)
    public void verificaPermissaoEscrita(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //Verifica se o usuário selecionou a opções de não perguntar novamente.
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST);
            }else{
                alerta.show();
            }
        }else{
            //Salva imagem
            salvarQRCode();
        }
    }

    // Callback da requisição de permissão
    @Override
    public void onRequestPermissionsResult(int codigoRequisicao, String permissoes[], int[] resultados) {
        // Verifica se esse retorno de resposta é referente a requisição de permissão da CAMERA
        if (codigoRequisicao == READ_EXTERNAL_STORAGE_PERMISSIONS_REQUEST) {
            if (resultados.length == 1 && resultados[0] == PackageManager.PERMISSION_GRANTED) {
                //Abre tela para seleção da imagem
                startActivityForResult(selecionarImagem, 1);
            } else {
                alerta.show();
            }
        }else{
            if (codigoRequisicao == WRITE_EXTERNAL_STORAGE_PERMISSIONS_REQUEST){
                if (resultados.length == 1 && resultados[0] == PackageManager.PERMISSION_GRANTED) {
                    //Salva imagem
                    salvarQRCode();
                } else {
                    alerta.show();
                }
            }else {
                super.onRequestPermissionsResult(codigoRequisicao, permissoes, resultados);
            }
        }
    }

    //Recebe a resposta da seleção de imagem.
    @Override
    protected void onActivityResult(int codigoRequisicao, int codigoResultado, Intent imagem) {
        super.onActivityResult(codigoRequisicao, codigoResultado, imagem);
        if(codigoRequisicao == 1 && codigoResultado == RESULT_OK){
            imagemSelecionada = imagem.getData();
            Picasso.with(ActPets.this).load(imagemSelecionada).transform(new TransformacaoCirculo()).into(ivFotoAnimal);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Carrega layout do toolbar
        getMenuInflater().inflate(R.menu.toolbar_principal, menu);
        MenuItem item = menu.findItem(R.id.menuNotificacao);
        item.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Trata click dos menus do toolbar
        switch (item.getItemId()) {
            case R.id.menuConfiguracoes:
                return true;
            case R.id.menuNotificacao:
                return true;
            case R.id.menuSair:
                //Limpa SharedPreferences
                GerenciadorSharedPreferences.setEmail(getBaseContext(), "");

                //Chama tela de login
                Intent principal = new Intent(ActPets.this, ActLogin.class);
                startActivity(principal);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //Configura tabs da tela de pet
    public void configuraTabs(){
        //Adiciona as opções nas tabs da tela principal
        TabHost abas = (TabHost) findViewById(R.id.tbPrincipal);

        abas.setup();

        TabHost.TabSpec descritor = abas.newTabSpec("Dados");
        descritor.setContent(R.id.llDados);
        descritor.setIndicator("", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_dados, getTheme()));
        abas.addTab(descritor);

        descritor = abas.newTabSpec("Compromissos");
        descritor.setContent(R.id.llCompromissos);
        descritor.setIndicator("", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_calendario, getTheme()));
        abas.addTab(descritor);

        descritor = abas.newTabSpec("QRCode");
        descritor.setContent(R.id.llQRCode);
        descritor.setIndicator("", ResourcesCompat.getDrawable(getResources(), R.drawable.ic_qrcode, getTheme()));
        abas.addTab(descritor);

        //Seta o fundo da primeira tab selecionada
        tabSelecionada = abas.getCurrentTab();
        abas.getTabWidget().getChildAt(abas.getCurrentTab()).setBackgroundColor(ContextCompat.getColor(getBaseContext(),R.color.buttonColorPrimary));

        abas.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

            public void onTabChanged(String arg0) {
                //Seta a cor de fundo da tab selecionada
                TabHost abas = (TabHost) findViewById(R.id.tbPrincipal);
                for (int i = 0; i < abas.getTabWidget().getChildCount(); i++) {
                    abas.getTabWidget().getChildAt(i).setBackgroundColor(ContextCompat.getColor(getBaseContext(),R.color.colorPrimary));
                }
                abas.getTabWidget().getChildAt(abas.getCurrentTab()).setBackgroundColor(ContextCompat.getColor(getBaseContext(),R.color.buttonColorPrimary));

                //Anima a transição de tabs
                View viewSelecionada = abas.getCurrentView();
                if (abas.getCurrentTab() > tabSelecionada)
                {
                    viewSelecionada.setAnimation(direita());
                }
                else
                {
                    viewSelecionada.setAnimation(esquerda());
                }
                tabSelecionada = abas.getCurrentTab();

            }

        });
    }

    //Anima a transição vinda da direita
    public Animation direita() {
        Animation direita = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        direita.setDuration(240);
        direita.setInterpolator(new AccelerateInterpolator());
        return direita;
    }

    //Anima a transição vinda da esquerda
    public Animation esquerda() {
        Animation esquerda = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        esquerda.setDuration(240);
        esquerda.setInterpolator(new AccelerateInterpolator());
        return esquerda;
    }

    //Recupera os dados de raça e espécie
    public void carregaSpinners(){
        especies.clear();
        especies.add(new Especie(0, "Selecione a espécie"));
        //Carrega lista de espécies
        new RequisicaoAsyncTask().execute("ListaEspecies", "0", "");
    }

    //Recupera os dados do pet selecionado
    public void recuperaDadosPet(){
        //Constrói mensagem de diálogo.
        dialogo = new AlertDialog.Builder(ActPets.this);
        dialogo.setIcon(R.mipmap.ic_launcher);
        //Apresenta mensagem de aviso ao usuário
        dialogo.setMessage("Para usar essa função é necessário que o aplicativo tenha permissão de acesso ao armazenamento de arquivos!");
        dialogo.setTitle("Aviso!");
        dialogo.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alerta = dialogo.create();

        //Inicia variavel para seleção de imagem
        selecionarImagem = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        //Carrega foto do pet selecionado.
        ivFotoAnimal = (ImageView) findViewById(R.id.ivFotoAnimal);
        Picasso.with(getBaseContext()).load(animal.getFoto()).transform(new TransformacaoCirculo()).into(ivFotoAnimal);
        ivFotoAnimal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Verifica permissões somente se a API for 23 ou maior
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    verificaPermissao();
                }
            }
        });

        //Carrega nome do pet.
        etNome = (EditText) findViewById(R.id.etNome);
        etNome.setText(animal.getNome());

        //Carrega nome do pet selecionado.
        tvNomePet = (TextView) findViewById(R.id.tvNomePet);
        tvNomePet.setText(animal.getNome());

        //Carrega raça do pet selecionado.
        tvRacaPet = (TextView) findViewById(R.id.tvRacaPet);
        tvRacaPet.setText(animal.getRaca().getNome());

        //Adiciona evento de click no botão de deletar pet.
        ivRemover = (ImageView) findViewById(R.id.ivRemover);
        ivRemover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Monta caixa de dialogo de confirmação de deleção.
                AlertDialog.Builder dialogo = new AlertDialog.Builder(ActPets.this);
                dialogo.setTitle("Aviso!")
                        .setMessage("Você tem certeza que deseja apagar esse pet? Todos os compromissos relacionados a esse pet também serão apagados.")
                        .setIcon(R.mipmap.ic_launcher)
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                pd = ProgressDialog.show(ActPets.this, "", "Por favor, aguarde...", false);
                                processos++;
                                new RequisicaoAsyncTask().execute("ExcluiAnimal", String.valueOf(animal.getIdAnimal()), "");
                            }
                        })
                        .setNegativeButton("Não", null);
                AlertDialog alerta = dialogo.create();
                alerta.show();
            }
        });

        //Recupera espécies.
        spEspecie = (Spinner) findViewById(R.id.spEspecie);
        adEspecie = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,especies){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                //Seta cores nos items
                if(position == 0) {
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.placeholderColor));
                }else{
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.textColor));
                }
                return v;
            }

            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    //Desabilita o primeiro item da lista.
                    //O primeiro item será usado para a dica.
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Coloca cor cinza no texto
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    //Coloca cor preta no texto
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spEspecie.setAdapter(adEspecie);

        //Adiciona evento de item selecionado no spinner de especie
        spEspecie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    pd = ProgressDialog.show(ActPets.this, "", "Por favor, aguarde...", false);
                    racas.clear();
                    racas.add(new Raca(0, "Selecione a raça", "", null));
                    spRaca.setSelection(0);
                    processos++;
                    new RequisicaoAsyncTask().execute("ListaRacasPorEspecie", String.valueOf(especies.get(position).getIdEspecie()), "");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Recupera raças.
        spRaca = (Spinner) findViewById(R.id.spRaca);
        racas.clear();
        racas.add(new Raca(0, "Selecione a raça", "", null));
        spRaca.setSelection(0);
        adRaca = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,racas){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                //Seta cores nos items
                if(position == 0) {
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.placeholderColor));
                }else{
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.textColor));
                }

                return v;
            }

            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    //Desabilita o primeiro item da lista.
                    //O primeiro item será usado para a dica.
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Coloca cor cinza no texto
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    //Coloca cor preta no texto
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spRaca.setAdapter(adRaca);

        //Recupera gênero.
        String[] generos = new String[]{"Selecione o gênero","Macho","Fêmea"};
        spGenero = (Spinner) findViewById(R.id.spGenero);
        ArrayAdapter adGenero = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,generos){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                //Seta cores nos items
                if(position == 0) {
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.placeholderColor));
                }else{
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.textColor));
                }

                return v;
            }

            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    //Desabilita o primeiro item da lista.
                    //O primeiro item será usado para a dica.
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Coloca cor cinza no texto
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    //Coloca cor preta no texto
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spGenero.setAdapter(adGenero);
        int pGenero = 0;
        for(int i=0;i<generos.length;i++){
            if(generos[i].equals(animal.getGenero())){
                pGenero = i;
                break;
            }
        }
        spGenero.setSelection(pGenero);

        //Recupera porte.
        String[] portes = new String[]{"Selecione o porte","Pequeno","Médio","Grande"};
        spPorte = (Spinner) findViewById(R.id.spPorte);
        ArrayAdapter adPorte = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,portes){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                //Seta cores nos items
                if(position == 0) {
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.placeholderColor));
                }else{
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.textColor));
                }

                return v;
            }

            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    //Desabilita o primeiro item da lista.
                    //O primeiro item será usado para a dica.
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Coloca cor cinza no texto
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    //Coloca cor preta no texto
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spPorte.setAdapter(adPorte);
        int pPorte = 0;
        for(int i=0;i<portes.length;i++){
            if(portes[i].equals(animal.getPorte())){
                pPorte = i;
                break;
            }
        }
        spPorte.setSelection(pPorte);

        //Recupera cor.
        String[] cores = new String[]{"Selecione a cor do animal","Água","Água-marinha média","Alizarina","Amarelo esverdeado","Amarelo queimado","Âmbar","Ameixa","Ametista","Aspargo","Azul","Azul ardósia","Azul ardósia claro","Azul ardósia escuro","Azul ardósia médio","Azul areado","Azul aço","Azul aço claro","Azul cadete","Azul camarada","Azul celeste brilhante","Azul claro","Azul cobalto","Azul céu","Azul céu claro","Azul céu profundo","Azul escuro","Azul flor de milho","Azul força aérea","Azul furtivo","Azul manteiga","Azul marinho","Azul meia-noite","Azul médio","Azul petróleo","Azul real","Azul taparuere","Azul violeta","Açafrão","Bordô","Borgonha","Bronze","Caramelo","Cardo","Carmesim","Carmim","Castanho avermelhado","Castanho claro","Cenoura","Cereja","Cereja Hollywood","Chocolate","Ciano","Ciano escuro","Cinza","Cinza ardósia","Cinza ardósia claro","Cinza ardósia escuro","Cinza escuro","Cinza fosco","Cobre","Coral","Coral claro","Couro","Caqui escuro","Dainise","Dourado","Dourado escuro","Escarlate","Esmeralda","Feldspato","Ferrugem","Fuligem","Fúchsia","Grená","Herbal","Índigo","Jade","Jambo","Jabuti preto","Laranja","Laranja escuro","Lilás","Limão (cor)","Madeira","Magenta","Magenta escuro","Malva","Marrom","Marrom amarelado","Marrom claro","Marrom rosado","Marrom sela","Naval","Ocre","Oliva","Oliva escura","Oliva parda","Orquídea","Orquídea escura","Orquídea média","Ouro","Pele","Prata","Preto","Púrpura","Púrpura média","Quantum","Rosa","Rosa brilhante","Rosa chocante","Rosa claro","Rosa forte","Rosa profundo","Roxo","Rútilo","Salmão","Salmão claro","Salmão escuro","Siena","Sépia","Terracota","Tijolo refratário","Tomate","Triássico","Turquesa","Turquesa escura","Turquesa média","Urucum","Verde","Verde espectro","Verde amarelado","Verde claro","Verde escuro","Verde floresta","Verde lima","Verde grama","Verde mar claro","Verde mar escuro","Verde mar médio","Verde militar","Verde Paris","Verde primavera","Verde primavera médio","Verde pálido","Verde-azulado","Vermelho","Vermelho escuro","Vermelho indiano","Vermelho violeta","Vermelho violeta médio","Vermelho violeta pálido","Violeta","Violeta escuro"};
        spCor = (Spinner) findViewById(R.id.spCor);
        ArrayAdapter adCor = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,cores){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                //Seta cores nos items
                if(position == 0) {
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.placeholderColor));
                }else{
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.textColor));
                }

                return v;
            }

            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    //Desabilita o primeiro item da lista.
                    //O primeiro item será usado para a dica.
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Coloca cor cinza no texto
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    //Coloca cor preta no texto
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spCor.setAdapter(adCor);
        int pCor = 0;

        //Carrega cor do animal
        for(int i=0;i<cores.length;i++)
        {
            if(cores[i].equals(animal.getCor())){
                pCor = i;
                break;
            }
        }
        spCor.setSelection(pCor);

        //Recupera idade.
        String[] idades = new String[]{"Selecione a idade do animal","Até 1 ano","1 ano","2 anos","3 anos","4 anos","5 anos","6 anos","7 anos","8 anos","9 anos","10 anos","11 anos","12 anos","13 anos","14 anos","15 anos","16 anos","17 anos","18 anos","19 anos","20 anos","21 anos","22 anos","23 anos","24 anos","25 anos","26 anos","27 anos","28 anos","29 anos","30 anos"};
        spIdade = (Spinner) findViewById(R.id.spIdade);
        ArrayAdapter adIdade = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,idades){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                //Seta cores nos items
                if(position == 0) {
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.placeholderColor));
                }else{
                    ((TextView) v).setTextColor(ContextCompat.getColor(getBaseContext(), R.color.textColor));
                }

                return v;
            }

            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    //Desabilita o primeiro item da lista.
                    //O primeiro item será usado para a dica.
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Coloca cor cinza no texto
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    //Coloca cor preta no texto
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spIdade.setAdapter(adIdade);
        int pIdade = 0;

        //Carrega idade do animal
        for(int i=0;i<idades.length;i++)
        {
            if (animal.getIdade() == 0)
                pIdade = 0;
            else if(idades[i].replace("Até ","").replace(" ano", "").replace("s", "").equals(String.valueOf(animal.getIdade())))
            {
                pIdade = i;
                break;
            }
        }
        spIdade.setSelection(pIdade);

        //Carrega características do pet.
        etCaracteristicas = (EditText) findViewById(R.id.etCaracteristicas);
        etCaracteristicas.setText(animal.getCaracteristicas());

        //Carrega flag de desaparecimento.
        cbDesaparecido = (CheckBox) findViewById(R.id.cbDesaparecido);
        cbDesaparecido.setChecked(animal.isDesaparecido());

        //Adiciona evento de click no botão de salvar
        btSalvar = (Button) findViewById(R.id.btSalvar);
        btSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AtualizaPet();
            }
        });
    }

    //Lista os compromissos do pet
    public void listaCompromissos()
    {
        adpEventos = new ArrayAdapter<Evento>(this,R.layout.item_compromissos_pet){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null)
                    convertView = getLayoutInflater().inflate(R.layout.item_compromissos_pet, null); /* obtém o objeto que está nesta posição do ArrayAdapter */

                final int index = position;

                ImageView ivIconeEvento = (ImageView) convertView.findViewById(R.id.ivIconeEvento);
                TextView tvNomeCompromisso = (TextView) convertView.findViewById(R.id.tvNomeCompromisso);
                TextView tvInformacao = (TextView) convertView.findViewById(R.id.tvInformacao);

                Evento evento = (Evento)getItem(position);
                evento1 = evento;

                if (evento.getTipo().equals("Compromisso")) {
                    tvNomeCompromisso.setText(evento.getNome());
                    Picasso.with(getContext()).load(R.drawable.ic_compromisso).into(ivIconeEvento);
                    tvInformacao.setText("Local: " + evento.getCompromisso().getNomelocal() + "\nData: " + transformaData(evento.getCompromisso().getDatahora()));
                }
                else if (evento.getTipo().equals("Medicamento")) {
                    tvNomeCompromisso.setText(evento.getNome());
                    Picasso.with(getContext()).load(R.drawable.ic_medicamento).into(ivIconeEvento);
                    tvInformacao.setText("Inicio: " + transformaData(evento.getMedicamento().getInicio()) +  "\nFim: " +
                            transformaData(evento.getMedicamento().getFim()));
                }
                else if (evento.getTipo().equals("Vacina")) {
                    tvNomeCompromisso.setText(evento.getNome());
                    Picasso.with(getContext()).load(R.drawable.ic_vacina).into(ivIconeEvento);
                    tvInformacao.setText("Aplicação: " + transformaData(evento.getVacina().getDataaplicacao()) + "\nValidade: " +
                            transformaData(evento.getVacina().getDatavalidade()));
                }

                //Adiciona evento de click no botão de deletar evento.
                ImageView ivRemover = (ImageView) convertView.findViewById(R.id.ivExcluirCompromisso);
                ivRemover.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        //Monta caixa de dialogo de confirmação de deleção.
                        AlertDialog.Builder dialogo = new AlertDialog.Builder(ActPets.this);
                        dialogo.setTitle("Aviso!")
                                .setMessage("Você tem certeza que deseja apagar esse evento?")
                                .setIcon(R.mipmap.ic_launcher)
                                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
/* = ProgressDialog.show(ActPets.this, "", "Por favor, aguarde...", false);
                                        processos++;
                                        if (evento1.getTipo().equals("Compromisso")) {
                                            new RequisicaoAsyncTask().execute("ExcluiCompromisso", String.valueOf(evento1.getIdEvento()), "");
                                        }
                                        else if (evento1.getTipo().equals("Medicamento")) {
                                            new RequisicaoAsyncTask().execute("ExcluiMedicamento", String.valueOf(evento1.getIdEvento()), "");;
                                        }
                                        else if (evento1.getTipo().equals("Vacina")) {
                                            new RequisicaoAsyncTask().execute("ExcluiVacina", String.valueOf(evento1.getIdEvento()), "");
                                        }*/

                                    }
                                })
                                .setNegativeButton("Não", null);
                        AlertDialog alerta = dialogo.create();
                        alerta.show();
                    }
                });

                return convertView;
            }
        };

        lvEventos = (ListView)findViewById(R.id.lvCompromissos);
        lvEventos.setAdapter(adpEventos);

        //Adiciona o evento de click nos items da lista
        /*lvEventos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               /* try {
                    Evento item = (Animal) parent.getItemAtPosition(position);
                    Intent configuracoes = new Intent(ActPets.this, ActPets.class);
                    configuracoes.putExtra("Animal", item.animalToJson().toString());
                    startActivity(configuracoes);
                } catch (Exception ex) {
                    Log.e("Erro", ex.getMessage());
                    Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
                }
            }
        });*/

        //Evento click do botão flutuante de adicionar pets
       /* FloatingActionButton button = (FloatingActionButton)findViewById(R.id.fbAddPet);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ActPets.this, ActCadastroPet.class);
                startActivity(i);
            }
        });*/


        //Carrega lista de eventos do pet do usuário
        //processos++;
        listaEventos.clear();
        try {
            JSONObject json = new JSONObject();
            json.put("idAnimal", animal.getIdAnimal());
            new RequisicaoAsyncTask().execute("ListaEventosPorAnimal", "0", json.toString());
        }catch(Exception ex){
            Log.e("Erro", ex.getMessage());
            Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
        }
    }

    public String transformaData(String data1)
    {
        String format = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date(data1.replaceAll("-", "/")));
    }

    //Carrega QRCode do pet
    public void carregaQRCode(){
        ivQRCode = (ImageView) findViewById(R.id.ivQRCode);

        if(animal.getQrcode().equals("")){
            //Gera endereço do QRCode
            String endereco = controlador.QRCode.urlQRCode + animal.getIdAnimal();
            //Gera QRCode
            Bitmap QRCode = controlador.QRCode.gerarQRCode(endereco);
            //Converte QRCode para base64
            String arquivo = Imagem.qrCodeEncode(QRCode);

            try {
                JSONObject json = new JSONObject();
                json.put("QRCode", arquivo);
                new RequisicaoAsyncTask().execute("AtualizaQRCode", String.valueOf(animal.getIdAnimal()), json.toString());
            }catch(Exception ex){
                Log.e("Erro", ex.getMessage());
                Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
            }
        }else {
            Picasso.with(getBaseContext()).load(animal.getQrcode()).into(ivQRCode);
        }

        //Adiciona evento de clique no botão de salvar QRCode.
        Button btSalvarQRCode = (Button) findViewById(R.id.btSalvarQRCode);
        btSalvarQRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verificaPermissaoEscrita();
            }
        });
    }

    //Valida informações e cadastra o pet
    public void AtualizaPet(){
        String erro = "";

        //Valida dados fornecidos
        if(etNome.getText().toString().trim().equals("")){
            erro = "Preencha o nome!";
        }else{
            if(spCor.getSelectedItemPosition() == 0){
                erro = "Preencha a cor!";
            }else{
                if(spIdade.getSelectedItemPosition() == 0){
                    erro = "Preencha a idade!";
                }else{
                    if(spEspecie.getSelectedItemPosition() == 0){
                        erro = "Selecione a espécie!";
                    }else{
                        if(spRaca.getSelectedItemPosition() == 0){
                            erro = "Selecione a raça!";
                        }else{
                            if(spGenero.getSelectedItemPosition() == 0){
                                erro = "Selecione o gênero!";
                            }else{
                                if(spPorte.getSelectedItemPosition() == 0){
                                    erro = "Selecione o porte!";
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            //Verifica se foi encontrado algum problema
            if (erro.equals("")) {
                JSONObject json = new JSONObject();
                String idade = "";
                json.put("Nome", etNome.getText().toString());
                json.put("Genero", spGenero.getSelectedItem().toString());
                json.put("Cor", spCor.getSelectedItem().toString());
                json.put("Porte", spPorte.getSelectedItem().toString());

                if (spIdade.getSelectedItem().toString() == "Até 1 ano")
                    idade = "0";
                else
                    idade = spIdade.getSelectedItem().toString().replace("Até ","").replace(" ano", "").replace("s", "");

                json.put("Idade", idade);

                json.put("Caracteristicas", etCaracteristicas.getText().toString());

                //Verifica se uma nova imagem foi selecionada para o pet
                if(imagemSelecionada != null) {
                    json.put("Foto", Imagem.fotoEncode(Imagem.recuperaCaminho(imagemSelecionada, ActPets.this)));
                }else{
                    json.put("Foto", "");
                }

                json.put("Desaparecido", cbDesaparecido.isChecked()?1:0);
                json.put("FotoCarteira", "");
                json.put("DataFotoCarteira", "");
                json.put("idUsuario", ActPrincipal.usuarioLogado.getIdUsuario());
                json.put("idRaca", ((Raca)spRaca.getSelectedItem()).getIdRaca());

                //Atualiza dados do animal
                pd = ProgressDialog.show(ActPets.this, "", "Por favor, aguarde...", false);
                processos++;
                new RequisicaoAsyncTask().execute("AtualizaAnimal", String.valueOf(animal.getIdAnimal()), json.toString());

                //Se o animal foi dado como desaparecido um novo registro de desaparecimento será gerado
                if(animal.isDesaparecido() != cbDesaparecido.isChecked() && cbDesaparecido.isChecked()){
                    processos++;

                    String data = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                    JSONObject json2 = new JSONObject();
                    json2.put("dataDesaparecimento",data);
                    json2.put("idAnimal", animal.getIdAnimal());
                    new RequisicaoAsyncTask().execute("InsereDesaparecimento", "0", json2.toString());
                }

            } else {
                Toast.makeText(ActPets.this, erro, Toast.LENGTH_LONG).show();
            }
        }catch (Exception ex){
            Log.e("Erro", ex.getMessage());
            Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
        }
    }

    private class RequisicaoAsyncTask extends AsyncTask<String, Void, JSONArray> {

        private String metodo;

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected JSONArray doInBackground(String... params) {
            JSONArray resultado = new JSONArray();

            try {
                //Recupera parâmetros e realiza a requisição
                metodo = params[0];
                int id = Integer.parseInt(params[1]);
                String conteudo = params[2];

                //Chama método da API
                resultado = Requisicao.chamaMetodo(metodo, id, conteudo);

            } catch (Exception e) {
                Log.e("Erro", e.getMessage());
                Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
            }
            return resultado;
        }

        @Override
        protected void onPostExecute(JSONArray resultado) {
            try {
                //Verifica se foi obtido algum resultado
                if(resultado.length() == 0)
                {
                    if (metodo == "ListaEventosPorAnimal")
                        Toast.makeText(ActPets.this, "Não foram encontrados eventos para este animal!", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    //Verifica se o objeto retornado foi uma mensagem ou um objeto
                    JSONObject json = resultado.getJSONObject(0);
                    if(Mensagem.isMensagem(json)){
                        if(!metodo.equals("InsereDesaparecimento")) {
                            Mensagem msg = Mensagem.jsonToMensagem(json);
                            Toast.makeText(ActPets.this, msg.getMensagem(), Toast.LENGTH_SHORT).show();

                            if (msg.getCodigo() == 10 || msg.getCodigo() == 11) {
                                //Chama tela principal
                                Intent intent = new Intent(ActPets.this, ActPrincipal.class);
                                startActivity(intent);
                            }
                        }
                    }else{
                        //Verifica qual foi o método chamado
                        if(metodo == "ListaEspecies") {
                            //Recupera especies
                            for(int i=0;i<resultado.length();i++){
                                especies.add(Especie.jsonToEspecie(resultado.getJSONObject(i)));
                            }

                            //Seleciona espécie do animal
                            int pEspecie = 0;
                            for(int i=0;i<especies.size();i++){
                                if(especies.get(i).getIdEspecie() == animal.getRaca().getEspecie().getIdEspecie()){
                                    pEspecie = i;
                                    break;
                                }
                            }
                            spEspecie.setSelection(pEspecie);
                        }else{
                            if(metodo == "ListaRacasPorEspecie"){
                                //Recupera racas
                                for(int i=0;i<resultado.length();i++){
                                    racas.add(Raca.jsonToRaca(resultado.getJSONObject(i)));
                                }

                                //Seleciona raça do animal
                                int pRaca = 0;
                                for(int i=0;i<racas.size();i++){
                                    if(racas.get(i).getIdRaca() == animal.getRaca().getIdRaca()){
                                        pRaca = i;
                                        break;
                                    }
                                }
                                spRaca.setSelection(pRaca);
                            }else{
                                if(metodo == "AtualizaQRCode") {
                                    JSONObject qrcode = resultado.getJSONObject(0);
                                    animal.setQrcode(qrcode.getString("QRCode"));
                                    Picasso.with(getBaseContext()).load(animal.getQrcode()).into(ivQRCode);
                                }
                                else{
                                    if(metodo == "ListaEventosPorAnimal") {
                                        //Monta lista de eventos dos animais do usuário logado
                                        for (int i = 0; i < resultado.length(); i++) {
                                            listaEventos.add(Evento.jsonToEvento(resultado.getJSONObject(i)));
                                        }
                                        adpEventos.clear();
                                        adpEventos.addAll(listaEventos);
                                    }
                                }
                            }

                        }
                    }
                }
            }
            catch (Exception e) {
                Log.e("Erro", e.getMessage());
                Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
            }

            processos--;
            if(processos == 0) {
                pd.dismiss();
            }
        }
    }

    public void salvarQRCode(){
        //Recupera bitmap do QRCode
        ivQRCode.buildDrawingCache();
        Bitmap qrcode = ivQRCode.getDrawingCache();

        //Salva QRCode como jpg
        OutputStream out = null;
        Uri uriArquivo;
        try {
            File pasta = new File(Environment.getExternalStorageDirectory() + File.separator + "Appet" + File.separator);
            pasta.mkdirs();
            File arquivo = new File(pasta, animal.getNome() + "_QRCODE.jpg");
            uriArquivo = Uri.fromFile(arquivo);
            out = new FileOutputStream(arquivo);
            qrcode.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            Toast.makeText(ActPets.this, "QRCode salvo!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Erro", e.getMessage());
            Toast.makeText(ActPets.this, "Não foi possível completar a operação!", Toast.LENGTH_SHORT).show();
        }
    }
}
