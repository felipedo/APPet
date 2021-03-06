package modelo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Diogo on 01/10/2016.
 */
public class Medicamento {
    private Evento evento;
    private String inicio, fim, frequenciadiaria, horasdeespera;

    public Medicamento(Evento evento, String horasdeespera, String inicio, String fim, String frequenciadiaria) {
        this.horasdeespera = horasdeespera;
        this.evento = evento;
        this.inicio = inicio;
        this.fim = fim;
        this.frequenciadiaria = frequenciadiaria;
    }

    public Evento getEvento() {
        return evento;
    }

    public void setEvento(Evento evento) {
        this.evento = evento;
    }

    public String getInicio() {
        return inicio;
    }

    public void setInicio(String inicio) {
        this.inicio = inicio;
    }

    public String getFim() {
        return fim;
    }

    public void setFim(String fim) {
        this.fim = fim;
    }

    public String getFrequenciadiaria() {
        return frequenciadiaria;
    }

    public void setFrequenciadiaria(String frequenciadiaria) {
        this.frequenciadiaria = frequenciadiaria;
    }

    public String getHorasdeespera() {
        return horasdeespera;
    }

    public void setHorasdeespera(String horasdeespera) {
        this.horasdeespera = horasdeespera;
    }

    public static Medicamento jsonToAnimal(JSONObject objeto) throws JSONException {
        if(objeto == null){
            return null;
        }else {

            Animal animal;
            Especie especie_t = new Especie(0,"");
            Raca raca = new Raca(0,"","",especie_t);
            Usuario usuario = new Usuario(0,"","","","","");

            //NomeAnimal nulo => ListaEventosPorAnimal => não precisa das informações do animal
            //    if (objeto.isNull("NomeAnimal"))
            animal = new Animal(0, "", "0", "0", "0", 0, "0", "0", "0", true,"0","0", usuario, raca);
            // else //NomeAnimal preenchido => ListaEventosPorUsuario => precisa das informações do animal
            // animal = new Animal(objeto.getInt("idAnimal"),objeto.getString("Nome"),objeto.getString("Genero"),objeto.getString("Cor"),objeto.getString("Porte"),objeto.getInt("Idade"),objeto.getString("Caracteristicas"),objeto.getString("QRCode"),objeto.getString("Foto"),objeto.getInt("Desaparecido") == 1?true:false,objeto.getString("FotoCarteira"),objeto.getString("DataFotoCarteira"),usuario,raca);


            Alerta alerta = new Alerta(objeto.getInt("idAlerta"),objeto.getString("NivelAlerta"),objeto.getInt("Frequencia"));
            Evento evento = new Evento(objeto.getInt("idEvento"),objeto.getString("Nome"),objeto.getString("Observacoes"),objeto.getInt("FlagAlerta"),alerta,animal,objeto.getString("Tipo"));;
            Medicamento medicamento = new Medicamento(evento,objeto.getString("HorasDeEspera"),objeto.getString("Inicio"),objeto.getString("Fim"),objeto.getString("FrequenciaDiaria"));
            return medicamento;
        }
    }

    public JSONObject medicamentoToJson() throws JSONException {
        //Evento
        JSONObject objeto = new JSONObject();
        objeto.put("idEvento",this.evento.getIdEvento());
        objeto.put("Nome",this.evento.getNome());
        objeto.put("Observacoes",this.evento.getObservacoes());
        objeto.put("FlagAlerta",this.evento.getFlagalerta());
        objeto.put("idAnimal",this.evento.getAnimal().getIdAnimal());
        objeto.put("Tipo",this.evento.getTipo());

        //Alerta
        objeto.put("idAlerta",this.evento.getAlerta().getidAlerta());
        objeto.put("NivelAlerta",this.evento.getAlerta().getNivelAlerta());
        objeto.put("Frequencia", this.evento.getAlerta().getFrequencia());

        //Medicamento
        objeto.put("Inicio",this.getInicio());
        objeto.put("Fim",this.getFim());
        objeto.put("FrequenciaDiaria",this.getFrequenciadiaria());
        objeto.put("HorasDeEspera",this.getHorasdeespera());
        return objeto;
    }
}
