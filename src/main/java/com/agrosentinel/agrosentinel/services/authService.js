import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL;

export const registrar = async (datosUsuario) => {
    try {
        const response = await axios.post(`${API_URL}/auth/registro`, datosUsuario);
        return response.data;
    } catch (error) {
        throw error.response?.data || "Error al conectar con el servidor";
    }
};