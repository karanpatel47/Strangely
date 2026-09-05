export default function MaintenancePage() {
    return (
        <div className="min-h-screen flex items-center justify-center bg-[#0b0b0f] px-6 text-white">
            <div className="max-w-xl text-center">
                <div className="text-6xl mb-6">✨</div>

                <h1 className="text-4xl md:text-5xl font-bold mb-4">
                    Something awesome is loading...
                </h1>

                <p className="text-lg text-gray-400 mb-8">
                    We’re upgrading Strangely behind the scenes
                    to bring you an even better experience.
                </p>

                <div className="inline-block rounded-2xl border border-white/10 bg-white/5 px-6 py-4">
                    <p className="text-sm text-gray-400">
                        We'll be back
                    </p>

                    <p className="text-xl font-semibold mt-1">
                        Tommorow at 10:00 PM 🚀
                    </p>
                </div>

                <p className="text-sm text-gray-500 mt-8">
                    Until then, stay curious. 👋
                </p>
            </div>
        </div>
    )
}
