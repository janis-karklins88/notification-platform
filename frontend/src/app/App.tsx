import { useEffect, useState } from 'react'
import { RouterProvider } from 'react-router-dom'
import { ToastContainer } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'

import { keycloak } from '../auth/keycloak'
import { router } from '../routes'

let keycloakInitPromise: Promise<boolean> | null = null

function initKeycloak() {
  if (!keycloakInitPromise) {
    keycloakInitPromise = keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
    })
  }

  return keycloakInitPromise
}

export function App() {
  const [isReady, setIsReady] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    let isMounted = true

    initKeycloak()
      .then(() => {
        if (isMounted) {
          setIsReady(true)
        }
      })
      .catch((error: unknown) => {
        console.error('Keycloak initialization failed', error)

        if (isMounted) {
          setErrorMessage('Authentication initialization failed.')
        }
      })

    return () => {
      isMounted = false
    }
  }, [])

  if (errorMessage) {
    return <div>{errorMessage}</div>
  }

  if (!isReady) {
    return <div>Loading authentication...</div>
  }

  return (
    <>
      <RouterProvider router={router} />
      <ToastContainer
        autoClose={3000}
        closeOnClick
        newestOnTop
        pauseOnFocusLoss={false}
        position="top-right"
        theme="light"
      />
    </>
  )
}
